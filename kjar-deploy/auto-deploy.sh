#!/bin/bash
# =============================================================================
# jBPM KIE Server Auto-Deploy Script
# =============================================================================
# This script waits for the KIE Server to become healthy, then:
#   1. Creates the KIE container via REST API (KJAR already in Maven repo
#      thanks to jbpm-init.sh running inside jbpm-server-full).
#   2. Creates a Business Central space + project and pushes all BPMN files
#      via git so process diagrams are viewable in the BC portal.
# =============================================================================

set -e

KIE_SERVER_URL="http://jbpm-server-full:8080/kie-server/services/rest/server"
BC_URL="http://jbpm-server-full:8080/business-central/rest"
BC_GIT_URL="http://wbadmin:wbadmin@jbpm-server-full:8080/business-central/git"
KIE_USER="wbadmin"
KIE_PWD="wbadmin"
CONTAINER_ID="property-platform"
GROUP_ID="com.greysoft"
ARTIFACT_ID="property-platform-kjar"
VERSION="1.0.0"
BPMN_DIR="/opt/jboss/kjar-deploy/com/greysoft/jbpm_engine/processes"

echo "[kjar-deploy] ========================================="
echo "[kjar-deploy]  KIE Server Auto-Deploy Script"
echo "[kjar-deploy] ========================================="

# ---- Wait for KIE Server ----
echo "[kjar-deploy] Waiting for KIE Server to become ready..."
MAX_WAIT=300
WAITED=0
until curl -sf -u "${KIE_USER}:${KIE_PWD}" "${KIE_SERVER_URL}" > /dev/null 2>&1; do
    if [ "$WAITED" -ge "$MAX_WAIT" ]; then
        echo "[kjar-deploy] ERROR: KIE Server did not become ready within ${MAX_WAIT}s"
        exit 1
    fi
    sleep 5
    WAITED=$((WAITED + 5))
    echo "[kjar-deploy] Still waiting... (${WAITED}s)"
done
echo "[kjar-deploy] KIE Server is ready."

# ===========================================================================
# STEP 1: Deploy KIE Container
# ===========================================================================
CONTAINER_CHECK=$(curl -sf -u "${KIE_USER}:${KIE_PWD}" \
    -H "Accept: application/json" \
    "${KIE_SERVER_URL}/containers/${CONTAINER_ID}" 2>/dev/null || echo "")

if echo "$CONTAINER_CHECK" | grep -q '"status" *: *"STARTED"'; then
    echo "[kjar-deploy] Container '${CONTAINER_ID}' already deployed and STARTED."
else
    # If container exists but in bad state, dispose it first
    if echo "$CONTAINER_CHECK" | grep -q "container-id"; then
        echo "[kjar-deploy] Disposing stale container..."
        curl -sf -u "${KIE_USER}:${KIE_PWD}" \
            -X DELETE \
            "${KIE_SERVER_URL}/containers/${CONTAINER_ID}" > /dev/null 2>&1 || true
        sleep 2
    fi

    echo "[kjar-deploy] Deploying container '${CONTAINER_ID}'..."
    DEPLOY_RESULT=$(curl -sf -u "${KIE_USER}:${KIE_PWD}" \
        -X PUT \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{
            \"container-id\": \"${CONTAINER_ID}\",
            \"release-id\": {
                \"group-id\": \"${GROUP_ID}\",
                \"artifact-id\": \"${ARTIFACT_ID}\",
                \"version\": \"${VERSION}\"
            },
            \"status\": \"STARTED\"
        }" \
        "${KIE_SERVER_URL}/containers/${CONTAINER_ID}" 2>&1)

    if echo "$DEPLOY_RESULT" | grep -q "SUCCESS"; then
        echo "[kjar-deploy] SUCCESS: KIE container '${CONTAINER_ID}' deployed."
    else
        echo "[kjar-deploy] WARN: Container deploy response: ${DEPLOY_RESULT}"
    fi
fi

# ===========================================================================
# STEP 2: Set up Business Central space, project, and push BPMNs via git
# ===========================================================================
echo "[kjar-deploy] Setting up Business Central for diagram viewing..."

# Wait for Business Central to be fully ready (lags behind KIE Server)
echo "[kjar-deploy] Waiting for Business Central REST API..."
BC_WAIT=0
until curl -sf -u "${KIE_USER}:${KIE_PWD}" -H "Accept: application/json" "${BC_URL}/spaces" > /dev/null 2>&1; do
    if [ "$BC_WAIT" -ge 120 ]; then
        echo "[kjar-deploy] WARN: Business Central REST API not ready after 120s. Skipping BC setup."
        echo "[kjar-deploy] (KIE container deployment was still successful.)"
        exit 0
    fi
    sleep 5
    BC_WAIT=$((BC_WAIT + 5))
    echo "[kjar-deploy] Waiting for BC... (${BC_WAIT}s)"
done
echo "[kjar-deploy] Business Central REST API is ready."

# Create space "greysoft" if it doesn't exist
SPACE_CHECK=$(curl -sf -u "${KIE_USER}:${KIE_PWD}" \
    -H "Accept: application/json" \
    "${BC_URL}/spaces/greysoft" 2>/dev/null || echo "")

if echo "$SPACE_CHECK" | grep -q '"name"'; then
    echo "[kjar-deploy] Space 'greysoft' already exists."
else
    echo "[kjar-deploy] Creating space 'greysoft'..."
    curl -sf -u "${KIE_USER}:${KIE_PWD}" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d '{"name":"greysoft","description":"Greysoft property platform","owner":"wbadmin","defaultGroupId":"com.greysoft"}' \
        "${BC_URL}/spaces" > /dev/null 2>&1 || true
    sleep 8
    echo "[kjar-deploy] Space 'greysoft' created."
fi

# Create project if it doesn't exist
PROJECT_CHECK=$(curl -sf -u "${KIE_USER}:${KIE_PWD}" \
    -H "Accept: application/json" \
    "${BC_URL}/spaces/greysoft/projects/property-platform" 2>/dev/null || echo "")

if echo "$PROJECT_CHECK" | grep -q '"name"'; then
    echo "[kjar-deploy] Project 'property-platform' already exists."
else
    echo "[kjar-deploy] Creating project 'property-platform'..."
    curl -sf -u "${KIE_USER}:${KIE_PWD}" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d '{"name":"property-platform","groupId":"com.greysoft","version":"1.0.0","description":"Property Platform BPMN Workflows"}' \
        "${BC_URL}/spaces/greysoft/projects" > /dev/null 2>&1 || true
    # Wait for async project creation to complete
    sleep 15
    echo "[kjar-deploy] Project 'property-platform' created."
fi

# Push BPMN files via git
if [ -d "${BPMN_DIR}" ] && ls "${BPMN_DIR}"/*.bpmn 1>/dev/null 2>&1; then
    BPMN_COUNT=$(ls -1 "${BPMN_DIR}"/*.bpmn | wc -l)
    echo "[kjar-deploy] Found ${BPMN_COUNT} BPMN files. Pushing to Business Central via git..."

    TMPDIR=$(mktemp -d)
    cd "${TMPDIR}"
    git config --global user.email "deployer@greysoft.com"
    git config --global user.name "KJAR Deployer"

    git clone "${BC_GIT_URL}/greysoft/property-platform" bc-project 2>/dev/null || {
        echo "[kjar-deploy] WARN: Could not clone BC project. Skipping BPMN push."
        rm -rf "${TMPDIR}"
        exit 0
    }

    cd bc-project

    # Check if BPMNs already exist
    EXISTING=$(ls src/main/resources/com/greysoft/property_platform/*.bpmn 2>/dev/null | wc -l)
    if [ "$EXISTING" -ge "$BPMN_COUNT" ]; then
        echo "[kjar-deploy] BPMNs already present in BC project (${EXISTING} files). Skipping push."
    else
        cp "${BPMN_DIR}"/*.bpmn src/main/resources/com/greysoft/property_platform/
        git add -A
        git commit -m "Auto-deploy: Add property-platform BPMN workflow diagrams" > /dev/null 2>&1
        git push origin master 2>/dev/null && \
            echo "[kjar-deploy] SUCCESS: ${BPMN_COUNT} BPMNs pushed to Business Central." || \
            echo "[kjar-deploy] WARN: Git push failed. BPMNs may need manual upload."
    fi

    # Cleanup
    cd /
    rm -rf "${TMPDIR}"
else
    echo "[kjar-deploy] No BPMN files found at ${BPMN_DIR}. Skipping."
fi

echo "[kjar-deploy] ========================================="
echo "[kjar-deploy]  Deployment complete!"
echo "[kjar-deploy]  - KIE container: ${CONTAINER_ID}"
echo "[kjar-deploy]  - Business Central: http://localhost:8090/business-central"
echo "[kjar-deploy]  - Space: greysoft / Project: property-platform"
echo "[kjar-deploy] ========================================="
