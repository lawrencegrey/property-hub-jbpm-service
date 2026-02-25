#!/bin/bash
# =============================================================================
# jBPM Server Init Script
# =============================================================================
# Runs BEFORE the main jBPM startup to:
#   1. Install the property-platform KJAR into the image's built-in Maven repo
#   2. Patch wbadmin roles to include rest-all and rest-project
#
# This avoids the need for a shared Maven repo volume (which would shadow the
# image's built-in Maven dependencies and break Business Central).
# =============================================================================

set -e

KJAR_DEPLOY_DIR="/opt/jboss/kjar-deploy"
MAVEN_TARGET="/opt/jboss/.m2/repository/com/greysoft/property-platform-kjar/1.0.0"
KIE_REALM_DIR="/opt/jboss/wildfly/standalone/configuration/kie-fs-realm-users"
# Elytron filesystem realm uses a hashed directory structure: first-char/second-char/name-HASH.xml
WBADMIN_REALM_FILE="${KIE_REALM_DIR}/w/b/wbadmin-O5RGCZDNNFXA.xml"

echo "[jbpm-init] === jBPM Init Script Starting ==="

# ---- 1. Install KJAR into local Maven repo ----
if [ -f "${KJAR_DEPLOY_DIR}/property-platform-kjar-1.0.0.jar" ]; then
    echo "[jbpm-init] Installing KJAR into Maven repo..."
    mkdir -p "${MAVEN_TARGET}"
    cp "${KJAR_DEPLOY_DIR}/property-platform-kjar-1.0.0.jar" \
       "${MAVEN_TARGET}/property-platform-kjar-1.0.0.jar"
    cp "${KJAR_DEPLOY_DIR}/property-platform-kjar-1.0.0.pom" \
       "${MAVEN_TARGET}/property-platform-kjar-1.0.0.pom"
    echo "[jbpm-init] KJAR installed at ${MAVEN_TARGET}"
else
    echo "[jbpm-init] WARNING: KJAR not found at ${KJAR_DEPLOY_DIR}. Skipping."
fi

# ---- 2. Patch wbadmin roles ----
if [ -f "${KJAR_DEPLOY_DIR}/wbadmin.xml" ]; then
    echo "[jbpm-init] Patching wbadmin roles at ${WBADMIN_REALM_FILE}..."
    if cp "${KJAR_DEPLOY_DIR}/wbadmin.xml" "${WBADMIN_REALM_FILE}" 2>/dev/null; then
        echo "[jbpm-init] wbadmin.xml patched with rest-all and rest-project roles."
    else
        echo "[jbpm-init] NOTE: Could not overwrite wbadmin.xml (likely already patched). Continuing."
    fi
else
    echo "[jbpm-init] WARNING: wbadmin.xml not found. Skipping role patch."
fi

echo "[jbpm-init] === Init complete. Starting jBPM server... ==="

# ---- 3. Hand off to the normal jBPM startup ----
exec ./start_jbpm-wb.sh
