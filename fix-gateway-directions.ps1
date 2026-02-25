
# Fix gateway directions in all BPMN files
# jBPM requires gatewayDirection attribute on all gateways

$bpmnDir = "src\main\resources\bpmn-diagrams"
$files = Get-ChildItem -Path $bpmnDir -Filter "*.bpmn"

$totalFixed = 0

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $modified = $false

    # Pattern: find gateway elements (exclusive, parallel, inclusive) without gatewayDirection
    # We need to determine direction based on incoming/outgoing count

    # Parse as XML
    [xml]$xml = $content
    $nsMgr = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
    $nsMgr.AddNamespace("bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    $nsMgr.AddNamespace("drools", "http://www.jboss.org/drools")
    $nsMgr.AddNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")

    # Find all gateway types
    $gatewayTypes = @("exclusiveGateway", "parallelGateway", "inclusiveGateway", "eventBasedGateway", "complexGateway")
    $fileFixed = 0

    foreach ($gwType in $gatewayTypes) {
        $gateways = $xml.SelectNodes("//bpmn2:$gwType", $nsMgr)
        foreach ($gw in $gateways) {
            # Count incoming and outgoing
            $incomingCount = ($gw.SelectNodes("bpmn2:incoming", $nsMgr)).Count
            $outgoingCount = ($gw.SelectNodes("bpmn2:outgoing", $nsMgr)).Count

            # Determine direction
            $direction = "Unspecified"
            if ($incomingCount -le 1 -and $outgoingCount -gt 1) {
                $direction = "Diverging"
            } elseif ($incomingCount -gt 1 -and $outgoingCount -le 1) {
                $direction = "Converging"
            } elseif ($incomingCount -gt 1 -and $outgoingCount -gt 1) {
                $direction = "Mixed"
            } elseif ($incomingCount -eq 1 -and $outgoingCount -eq 1) {
                # Pass-through gateway - treat as Diverging
                $direction = "Diverging"
            }

            # Check if gatewayDirection already exists
            $existingDir = $gw.GetAttribute("gatewayDirection")
            if ([string]::IsNullOrEmpty($existingDir) -or $existingDir -eq "Unspecified") {
                $gw.SetAttribute("gatewayDirection", $direction)
                $fileFixed++
                $modified = $true
                Write-Host "  $($gw.LocalName) $($gw.id): in=$incomingCount out=$outgoingCount -> $direction"
            }
        }
    }

    if ($modified) {
        # Save with UTF-8 encoding (no BOM)
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        $writer = New-Object System.IO.StreamWriter($file.FullName, $false, $utf8NoBom)
        $xml.Save($writer)
        $writer.Close()
        Write-Host "Fixed $fileFixed gateways in $($file.Name)"
        $totalFixed += $fileFixed
    }
}

Write-Host ""
Write-Host "Done! Fixed $totalFixed gateways across all BPMN files."
