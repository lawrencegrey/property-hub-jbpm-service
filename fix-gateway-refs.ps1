# Fix gateway directions AND ensure incoming/outgoing refs are correct
$bpmnDir = Join-Path $PSScriptRoot "src\main\resources\bpmn-diagrams"
$files = Get-ChildItem -Path $bpmnDir -Filter "*.bpmn"
$totalFixed = 0

foreach ($file in $files) {
    if ($file.Length -eq 0) { Write-Host "  Skipping empty file: $($file.Name)"; continue }
    $xmlDoc = New-Object System.Xml.XmlDocument
    try { $xmlDoc.Load($file.FullName) } catch { Write-Host "  Skipping invalid XML: $($file.Name)"; continue }

    $nsMgr = New-Object System.Xml.XmlNamespaceManager -ArgumentList @($xmlDoc.NameTable)
    $nsMgr.AddNamespace("bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL")

    $modified = $false
    $processes = $xmlDoc.SelectNodes("//bpmn2:process", $nsMgr)

    foreach ($proc in $processes) {
        # Build maps from sequence flows
        $outMap = @{}
        $inMap = @{}
        $seqFlows = $proc.SelectNodes("bpmn2:sequenceFlow", $nsMgr)
        foreach ($sf in $seqFlows) {
            $src = $sf.GetAttribute("sourceRef")
            $tgt = $sf.GetAttribute("targetRef")
            $fid = $sf.GetAttribute("id")
            if (-not $outMap.ContainsKey($src)) { $outMap[$src] = @() }
            $outMap[$src] += $fid
            if (-not $inMap.ContainsKey($tgt)) { $inMap[$tgt] = @() }
            $inMap[$tgt] += $fid
        }

        $gwTypes = @("exclusiveGateway","parallelGateway","inclusiveGateway","eventBasedGateway","complexGateway")
        foreach ($gwType in $gwTypes) {
            $gateways = $proc.SelectNodes("bpmn2:$gwType", $nsMgr)
            foreach ($gw in $gateways) {
                $gwId = $gw.GetAttribute("id")
                $expIn = if ($inMap.ContainsKey($gwId)) { $inMap[$gwId] } else { @() }
                $expOut = if ($outMap.ContainsKey($gwId)) { $outMap[$gwId] } else { @() }

                # Check current incoming
                $curIn = @()
                $inNodes = $gw.SelectNodes("bpmn2:incoming", $nsMgr)
                foreach ($n in $inNodes) { $curIn += $n.InnerText }

                $curOut = @()
                $outNodes = $gw.SelectNodes("bpmn2:outgoing", $nsMgr)
                foreach ($n in $outNodes) { $curOut += $n.InnerText }

                $needsFix = $false

                # Check incoming mismatch
                if ($curIn.Count -ne $expIn.Count) { $needsFix = $true }
                else { foreach ($e in $expIn) { if ($curIn -notcontains $e) { $needsFix = $true; break } } }

                # Check outgoing mismatch
                if ($curOut.Count -ne $expOut.Count) { $needsFix = $true }
                else { foreach ($e in $expOut) { if ($curOut -notcontains $e) { $needsFix = $true; break } } }

                # Determine direction
                $inC = $expIn.Count
                $outC = $expOut.Count
                if ($inC -le 1 -and $outC -gt 1) { $dir = "Diverging" }
                elseif ($inC -gt 1 -and $outC -le 1) { $dir = "Converging" }
                elseif ($inC -gt 1 -and $outC -gt 1) { $dir = "Mixed" }
                else { $dir = "Diverging" }

                $curDir = $gw.GetAttribute("gatewayDirection")
                if ($curDir -ne $dir) { $needsFix = $true }

                if ($needsFix) {
                    # Remove existing incoming/outgoing
                    foreach ($n in $gw.SelectNodes("bpmn2:incoming", $nsMgr)) { $gw.RemoveChild($n) | Out-Null }
                    foreach ($n in $gw.SelectNodes("bpmn2:outgoing", $nsMgr)) { $gw.RemoveChild($n) | Out-Null }

                    # Re-add incoming
                    foreach ($fid in $expIn) {
                        $el = $xmlDoc.CreateElement("bpmn2","incoming","http://www.omg.org/spec/BPMN/20100524/MODEL")
                        $el.InnerText = $fid
                        $gw.AppendChild($el) | Out-Null
                    }
                    # Add outgoing
                    foreach ($fid in $expOut) {
                        $el = $xmlDoc.CreateElement("bpmn2","outgoing","http://www.omg.org/spec/BPMN/20100524/MODEL")
                        $el.InnerText = $fid
                        $gw.AppendChild($el) | Out-Null
                    }

                    $gw.SetAttribute("gatewayDirection", $dir)
                    $modified = $true
                    $totalFixed++
                    Write-Host "  $($file.Name): $gwType $gwId in=$inC out=$outC dir=$dir"
                }
            }
        }
    }

    if ($modified) {
        $settings = New-Object System.Xml.XmlWriterSettings
        $settings.Indent = $true
        $settings.IndentChars = "  "
        $settings.Encoding = New-Object System.Text.UTF8Encoding($false)
        $writer = [System.Xml.XmlWriter]::Create($file.FullName, $settings)
        $xmlDoc.Save($writer)
        $writer.Close()
        Write-Host "  Saved $($file.Name)"
    }
}

Write-Host ""
Write-Host "Done! Fixed $totalFixed gateway issues."
