#!/usr/bin/env pwsh
# ============================================================
# Camunda Cloud BPMN → jBPM BPMN2 Converter
# Converts all .bpmn files in bpmn-diagrams/ (except make-offer.bpmn)
# ============================================================

$bpmnDir = "$PSScriptRoot\src\main\resources\bpmn-diagrams"
$files = Get-ChildItem -Path $bpmnDir -Filter "*.bpmn" | Where-Object { $_.Name -ne "make-offer.bpmn" }

Write-Host "Found $($files.Count) BPMN files to convert" -ForegroundColor Cyan

foreach ($file in $files) {
    Write-Host "`nConverting: $($file.Name)" -ForegroundColor Yellow
    
    [xml]$xml = Get-Content $file.FullName -Raw -Encoding UTF8
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    
    # ---- Extract process info from original XML ----
    $nsManager = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
    $nsManager.AddNamespace("bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL")
    $nsManager.AddNamespace("zeebe", "http://camunda.org/schema/zeebe/1.0")
    $nsManager.AddNamespace("bpmndi", "http://www.omg.org/spec/BPMN/20100524/DI")
    $nsManager.AddNamespace("dc", "http://www.omg.org/spec/DD/20100524/DC")
    $nsManager.AddNamespace("di", "http://www.omg.org/spec/DD/20100524/DI")
    $nsManager.AddNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance")
    
    # Get process elements
    $processes = $xml.SelectNodes("//bpmn:process", $nsManager)
    
    # Build the jBPM BPMN2 output
    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine('<?xml version="1.0" encoding="UTF-8"?>')
    [void]$sb.AppendLine('<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"')
    [void]$sb.AppendLine('                   xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"')
    [void]$sb.AppendLine('                   xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"')
    [void]$sb.AppendLine('                   xmlns:di="http://www.omg.org/spec/DD/20100524/DI"')
    [void]$sb.AppendLine('                   xmlns:drools="http://www.jboss.org/drools"')
    [void]$sb.AppendLine('                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')
    
    $defId = $xml.definitions.id
    if (-not $defId) { $defId = "Definitions_$($file.BaseName -replace '-','_')" }
    
    [void]$sb.AppendLine("                   id=`"$defId`"")
    [void]$sb.AppendLine('                   targetNamespace="http://www.jboss.org/drools"')
    [void]$sb.AppendLine('                   expressionLanguage="http://www.mvel.org/2.0"')
    [void]$sb.AppendLine('                   typeLanguage="http://www.java.com/javaTypes"')
    [void]$sb.AppendLine('                   xsi:schemaLocation="http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd">')
    [void]$sb.AppendLine('')
    
    # ---- Process each process element ----
    foreach ($proc in $processes) {
        $processId = $proc.id
        $processName = $proc.name
        if (-not $processName) { $processName = $processId }
        
        # Collect ALL variables used across all tasks (inputs + outputs)
        $allVars = @{}
        
        # Scan service tasks for zeebe:ioMapping
        $serviceTasks = $proc.SelectNodes("bpmn:serviceTask", $nsManager)
        foreach ($st in $serviceTasks) {
            $ioMapping = $st.SelectSingleNode("bpmn:extensionElements/zeebe:ioMapping", $nsManager)
            if ($ioMapping) {
                foreach ($input in $ioMapping.SelectNodes("zeebe:input", $nsManager)) {
                    $target = $input.GetAttribute("target")
                    if ($target -and -not $allVars.ContainsKey($target)) {
                        $allVars[$target] = "String"
                    }
                }
                foreach ($output in $ioMapping.SelectNodes("zeebe:output", $nsManager)) {
                    $target = $output.GetAttribute("target")
                    if ($target -and -not $allVars.ContainsKey($target)) {
                        $allVars[$target] = "String"
                    }
                }
            }
        }
        
        # Scan user tasks for zeebe:ioMapping
        $userTasks = $proc.SelectNodes("bpmn:userTask", $nsManager)
        foreach ($ut in $userTasks) {
            $ioMapping = $ut.SelectSingleNode("bpmn:extensionElements/zeebe:ioMapping", $nsManager)
            if ($ioMapping) {
                foreach ($input in $ioMapping.SelectNodes("zeebe:input", $nsManager)) {
                    $target = $input.GetAttribute("target")
                    if ($target -and -not $allVars.ContainsKey($target)) {
                        $allVars[$target] = "String"
                    }
                }
                foreach ($output in $ioMapping.SelectNodes("zeebe:output", $nsManager)) {
                    $target = $output.GetAttribute("target")
                    if ($target -and -not $allVars.ContainsKey($target)) {
                        $allVars[$target] = "String"
                    }
                }
            }
        }
        
        # Scan script tasks
        $scriptTasks = $proc.SelectNodes("bpmn:scriptTask", $nsManager)
        foreach ($scr in $scriptTasks) {
            $zeebeScript = $scr.SelectSingleNode("bpmn:extensionElements/zeebe:script", $nsManager)
            if ($zeebeScript) {
                $resultVar = $zeebeScript.GetAttribute("resultVariable")
                if ($resultVar -and -not $allVars.ContainsKey($resultVar)) {
                    $allVars[$resultVar] = "Boolean"
                }
            }
        }
        
        # Also check condition expressions for variable references
        $condExprs = $proc.SelectNodes(".//bpmn:conditionExpression", $nsManager)
        foreach ($ce in $condExprs) {
            $ceText = $ce.InnerText.Trim()
            if ($ceText.StartsWith("=")) {
                # Extract variable names from FEEL expressions
                $ceText = $ceText.Substring(1).Trim()
                # Patterns like "approved = true", "decision = false", etc.
                if ($ceText -match "^(\w+)\s*=") {
                    $varName = $Matches[1]
                    if (-not $allVars.ContainsKey($varName)) {
                        if ($ceText -match "= (true|false)") {
                            $allVars[$varName] = "Boolean"
                        } else {
                            $allVars[$varName] = "String"
                        }
                    }
                }
            }
        }
        
        # Detect boolean vars from known patterns
        foreach ($varName in @($allVars.Keys)) {
            if ($varName -in @("approved","decision","is_verified","is_accepted","is_kyc_document",
                              "offer_status","loan_financed","use_platform","verificationPassed",
                              "is_lawyer_exists","vehicle_validity","seller_interested",
                              "lawyer_approved_docs","seller_agreement_approve","is_insurer_included")) {
                $allVars[$varName] = "Boolean"
            }
        }
        
        # Write item definitions
        [void]$sb.AppendLine("  <!-- ============ Item Definitions (process variables) ============ -->")
        foreach ($varName in ($allVars.Keys | Sort-Object)) {
            $structRef = $allVars[$varName]
            [void]$sb.AppendLine("  <bpmn2:itemDefinition id=`"_${varName}Item`" structureRef=`"$structRef`" />")
        }
        [void]$sb.AppendLine('')
        
        # Write process header
        [void]$sb.AppendLine("  <!-- ============ Process ============ -->")
        [void]$sb.AppendLine("  <bpmn2:process id=`"$processId`" name=`"$processName`" isExecutable=`"true`"")
        [void]$sb.AppendLine("                 drools:packageName=`"com.greysoft.jbpm_engine.processes`">")
        [void]$sb.AppendLine('')
        
        # Write process-level properties
        [void]$sb.AppendLine("    <!-- Process-level variables -->")
        foreach ($varName in ($allVars.Keys | Sort-Object)) {
            [void]$sb.AppendLine("    <bpmn2:property id=`"$varName`" itemSubjectRef=`"_${varName}Item`" name=`"$varName`" />")
        }
        [void]$sb.AppendLine('')
        
        # ---- Convert each element ----
        
        # Start events
        $startEvents = $proc.SelectNodes("bpmn:startEvent", $nsManager)
        foreach ($se in $startEvents) {
            $seId = $se.id
            $seName = $se.GetAttribute("name")
            $nameAttr = if ($seName) { " name=`"$seName`"" } else { "" }
            [void]$sb.AppendLine("    <bpmn2:startEvent id=`"$seId`"$nameAttr>")
            foreach ($outgoing in $se.SelectNodes("bpmn:outgoing", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:outgoing>$($outgoing.InnerText)</bpmn2:outgoing>")
            }
            [void]$sb.AppendLine("    </bpmn2:startEvent>")
            [void]$sb.AppendLine('')
        }
        
        # User tasks
        foreach ($ut in $userTasks) {
            $utId = $ut.id
            $utName = [System.Security.SecurityElement]::Escape($ut.GetAttribute("name"))
            [void]$sb.AppendLine("    <bpmn2:userTask id=`"$utId`" name=`"$utName`">")
            
            # Incoming/outgoing
            foreach ($inc in $ut.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            foreach ($outg in $ut.SelectNodes("bpmn:outgoing", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:outgoing>$($outg.InnerText)</bpmn2:outgoing>")
            }
            
            # IO from zeebe:ioMapping - outputs become dataOutputAssociations
            $ioMapping = $ut.SelectSingleNode("bpmn:extensionElements/zeebe:ioMapping", $nsManager)
            if ($ioMapping) {
                $outputs = $ioMapping.SelectNodes("zeebe:output", $nsManager)
                if ($outputs.Count -gt 0) {
                    [void]$sb.AppendLine("      <bpmn2:ioSpecification id=`"ioSpec_$utId`">")
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        [void]$sb.AppendLine("        <bpmn2:dataOutput id=`"_${utId}_${target}Output`" name=`"$target`" />")
                    }
                    [void]$sb.AppendLine("        <bpmn2:inputSet id=`"inputSet_$utId`" />")
                    [void]$sb.AppendLine("        <bpmn2:outputSet id=`"outputSet_$utId`">")
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        [void]$sb.AppendLine("          <bpmn2:dataOutputRefs>_${utId}_${target}Output</bpmn2:dataOutputRefs>")
                    }
                    [void]$sb.AppendLine("        </bpmn2:outputSet>")
                    [void]$sb.AppendLine("      </bpmn2:ioSpecification>")
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        [void]$sb.AppendLine("      <bpmn2:dataOutputAssociation id=`"doa_${utId}_$target`">")
                        [void]$sb.AppendLine("        <bpmn2:sourceRef>_${utId}_${target}Output</bpmn2:sourceRef>")
                        [void]$sb.AppendLine("        <bpmn2:targetRef>$target</bpmn2:targetRef>")
                        [void]$sb.AppendLine("      </bpmn2:dataOutputAssociation>")
                    }
                }
            }
            
            [void]$sb.AppendLine("    </bpmn2:userTask>")
            [void]$sb.AppendLine('')
        }
        
        # Service tasks → bpmn2:task with drools:taskName
        foreach ($st in $serviceTasks) {
            $stId = $st.id
            $stName = [System.Security.SecurityElement]::Escape($st.GetAttribute("name"))
            
            # Get task type from zeebe:taskDefinition
            $taskDef = $st.SelectSingleNode("bpmn:extensionElements/zeebe:taskDefinition", $nsManager)
            $taskType = ""
            if ($taskDef) {
                $taskType = $taskDef.GetAttribute("type")
                # Clean up FEEL prefix
                if ($taskType.StartsWith("=")) { $taskType = $taskType.Substring(1) }
            }
            
            $taskNameAttr = ""
            if ($taskType) {
                $taskNameAttr = "`n               drools:taskName=`"$taskType`""
            }
            
            [void]$sb.AppendLine("    <bpmn2:task id=`"$stId`" name=`"$stName`"$taskNameAttr>")
            
            # Incoming/outgoing
            foreach ($inc in $st.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            foreach ($outg in $st.SelectNodes("bpmn:outgoing", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:outgoing>$($outg.InnerText)</bpmn2:outgoing>")
            }
            
            # IO mapping
            $ioMapping = $st.SelectSingleNode("bpmn:extensionElements/zeebe:ioMapping", $nsManager)
            if ($ioMapping) {
                $inputs = $ioMapping.SelectNodes("zeebe:input", $nsManager)
                $outputs = $ioMapping.SelectNodes("zeebe:output", $nsManager)
                
                if ($inputs.Count -gt 0 -or $outputs.Count -gt 0) {
                    [void]$sb.AppendLine("      <bpmn2:ioSpecification id=`"ioSpec_$stId`">")
                    
                    # Add TaskType input
                    if ($taskType) {
                        [void]$sb.AppendLine("        <bpmn2:dataInput id=`"_${stId}_TaskTypeInput`" drools:dtype=`"String`" name=`"TaskType`" />")
                    }
                    
                    foreach ($input in $inputs) {
                        $target = $input.GetAttribute("target")
                        $dtype = if ($allVars.ContainsKey($target)) { $allVars[$target] } else { "String" }
                        [void]$sb.AppendLine("        <bpmn2:dataInput id=`"_${stId}_${target}Input`" drools:dtype=`"$dtype`" name=`"$target`" />")
                    }
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        $dtype = if ($allVars.ContainsKey($target)) { $allVars[$target] } else { "String" }
                        [void]$sb.AppendLine("        <bpmn2:dataOutput id=`"_${stId}_${target}Output`" drools:dtype=`"$dtype`" name=`"$target`" />")
                    }
                    
                    [void]$sb.AppendLine("        <bpmn2:inputSet id=`"inputSet_$stId`">")
                    if ($taskType) {
                        [void]$sb.AppendLine("          <bpmn2:dataInputRefs>_${stId}_TaskTypeInput</bpmn2:dataInputRefs>")
                    }
                    foreach ($input in $inputs) {
                        $target = $input.GetAttribute("target")
                        [void]$sb.AppendLine("          <bpmn2:dataInputRefs>_${stId}_${target}Input</bpmn2:dataInputRefs>")
                    }
                    [void]$sb.AppendLine("        </bpmn2:inputSet>")
                    
                    [void]$sb.AppendLine("        <bpmn2:outputSet id=`"outputSet_$stId`">")
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        [void]$sb.AppendLine("          <bpmn2:dataOutputRefs>_${stId}_${target}Output</bpmn2:dataOutputRefs>")
                    }
                    [void]$sb.AppendLine("        </bpmn2:outputSet>")
                    [void]$sb.AppendLine("      </bpmn2:ioSpecification>")
                    
                    # Data input associations
                    if ($taskType) {
                        [void]$sb.AppendLine("      <bpmn2:dataInputAssociation id=`"dia_${stId}_TaskType`">")
                        [void]$sb.AppendLine("        <bpmn2:targetRef>_${stId}_TaskTypeInput</bpmn2:targetRef>")
                        [void]$sb.AppendLine("        <bpmn2:assignment id=`"assign_${stId}_TaskType`">")
                        [void]$sb.AppendLine("          <bpmn2:from xsi:type=`"bpmn2:tFormalExpression`">$taskType</bpmn2:from>")
                        [void]$sb.AppendLine("          <bpmn2:to xsi:type=`"bpmn2:tFormalExpression`">_${stId}_TaskTypeInput</bpmn2:to>")
                        [void]$sb.AppendLine("        </bpmn2:assignment>")
                        [void]$sb.AppendLine("      </bpmn2:dataInputAssociation>")
                    }
                    
                    foreach ($input in $inputs) {
                        $target = $input.GetAttribute("target")
                        $source = $input.GetAttribute("source")
                        
                        # Determine if source is a literal or a process variable reference
                        $isLiteral = $false
                        $literalValue = ""
                        
                        if ($source.StartsWith("=")) {
                            $sourceExpr = $source.Substring(1).Trim()
                            # Check if it's a simple var reference
                            if ($sourceExpr -match '^\w+$') {
                                # Simple variable reference
                                [void]$sb.AppendLine("      <bpmn2:dataInputAssociation id=`"dia_${stId}_$target`">")
                                [void]$sb.AppendLine("        <bpmn2:sourceRef>$sourceExpr</bpmn2:sourceRef>")
                                [void]$sb.AppendLine("        <bpmn2:targetRef>_${stId}_${target}Input</bpmn2:targetRef>")
                                [void]$sb.AppendLine("      </bpmn2:dataInputAssociation>")
                            } else {
                                # Complex expression or literal - use assignment
                                # Escape XML special chars in expressions
                                $escapedExpr = [System.Security.SecurityElement]::Escape($sourceExpr)
                                [void]$sb.AppendLine("      <bpmn2:dataInputAssociation id=`"dia_${stId}_$target`">")
                                [void]$sb.AppendLine("        <bpmn2:targetRef>_${stId}_${target}Input</bpmn2:targetRef>")
                                [void]$sb.AppendLine("        <bpmn2:assignment id=`"assign_${stId}_$target`">")
                                [void]$sb.AppendLine("          <bpmn2:from xsi:type=`"bpmn2:tFormalExpression`">#{$escapedExpr}</bpmn2:from>")
                                [void]$sb.AppendLine("          <bpmn2:to xsi:type=`"bpmn2:tFormalExpression`">_${stId}_${target}Input</bpmn2:to>")
                                [void]$sb.AppendLine("        </bpmn2:assignment>")
                                [void]$sb.AppendLine("      </bpmn2:dataInputAssociation>")
                            }
                        } else {
                            # Plain source (no = prefix) - treat as variable
                            [void]$sb.AppendLine("      <bpmn2:dataInputAssociation id=`"dia_${stId}_$target`">")
                            [void]$sb.AppendLine("        <bpmn2:sourceRef>$source</bpmn2:sourceRef>")
                            [void]$sb.AppendLine("        <bpmn2:targetRef>_${stId}_${target}Input</bpmn2:targetRef>")
                            [void]$sb.AppendLine("      </bpmn2:dataInputAssociation>")
                        }
                    }
                    
                    # Data output associations
                    foreach ($output in $outputs) {
                        $target = $output.GetAttribute("target")
                        [void]$sb.AppendLine("      <bpmn2:dataOutputAssociation id=`"doa_${stId}_$target`">")
                        [void]$sb.AppendLine("        <bpmn2:sourceRef>_${stId}_${target}Output</bpmn2:sourceRef>")
                        [void]$sb.AppendLine("        <bpmn2:targetRef>$target</bpmn2:targetRef>")
                        [void]$sb.AppendLine("      </bpmn2:dataOutputAssociation>")
                    }
                }
            }
            
            [void]$sb.AppendLine("    </bpmn2:task>")
            [void]$sb.AppendLine('')
        }
        
        # Script tasks
        foreach ($scr in $scriptTasks) {
            $scrId = $scr.id
            $scrName = [System.Security.SecurityElement]::Escape($scr.GetAttribute("name"))
            
            $zeebeScript = $scr.SelectSingleNode("bpmn:extensionElements/zeebe:script", $nsManager)
            $expression = ""
            $resultVar = ""
            if ($zeebeScript) {
                $expression = $zeebeScript.GetAttribute("expression")
                $resultVar = $zeebeScript.GetAttribute("resultVariable")
                if ($expression.StartsWith("=")) { $expression = $expression.Substring(1) }
            }
            
            [void]$sb.AppendLine("    <bpmn2:scriptTask id=`"$scrId`" name=`"$scrName`" scriptFormat=`"http://www.java.com/java`">")
            
            foreach ($inc in $scr.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            foreach ($outg in $scr.SelectNodes("bpmn:outgoing", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:outgoing>$($outg.InnerText)</bpmn2:outgoing>")
            }
            
            if ($expression -and $resultVar) {
                $escapedExpr = [System.Security.SecurityElement]::Escape($expression)
                [void]$sb.AppendLine("      <bpmn2:script>kcontext.setVariable(`"$resultVar`", $escapedExpr);</bpmn2:script>")
            }
            
            [void]$sb.AppendLine("    </bpmn2:scriptTask>")
            [void]$sb.AppendLine('')
        }
        
        # Gateways (exclusive, parallel, inclusive)
        foreach ($gwType in @("exclusiveGateway","parallelGateway","inclusiveGateway")) {
            $gateways = $proc.SelectNodes("bpmn:$gwType", $nsManager)
            foreach ($gw in $gateways) {
                $gwId = $gw.id
                $gwName = [System.Security.SecurityElement]::Escape($gw.GetAttribute("name"))
                $nameAttr = if ($gwName) { " name=`"$gwName`"" } else { "" }
                $gatewayDefault = $gw.GetAttribute("default")
                $defaultAttr = if ($gatewayDefault) { " default=`"$gatewayDefault`"" } else { "" }
                
                [void]$sb.AppendLine("    <bpmn2:$gwType id=`"$gwId`"$nameAttr$defaultAttr>")
                foreach ($inc in $gw.SelectNodes("bpmn:incoming", $nsManager)) {
                    [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
                }
                foreach ($outg in $gw.SelectNodes("bpmn:outgoing", $nsManager)) {
                    [void]$sb.AppendLine("      <bpmn2:outgoing>$($outg.InnerText)</bpmn2:outgoing>")
                }
                [void]$sb.AppendLine("    </bpmn2:$gwType>")
                [void]$sb.AppendLine('')
            }
        }
        
        # End events
        $endEvents = $proc.SelectNodes("bpmn:endEvent", $nsManager)
        foreach ($ee in $endEvents) {
            $eeId = $ee.id
            $eeName = $ee.GetAttribute("name")
            $nameAttr = if ($eeName) { " name=`"$eeName`"" } else { "" }
            [void]$sb.AppendLine("    <bpmn2:endEvent id=`"$eeId`"$nameAttr>")
            foreach ($inc in $ee.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            [void]$sb.AppendLine("    </bpmn2:endEvent>")
            [void]$sb.AppendLine('')
        }
        
        # Intermediate throw events
        $throwEvents = $proc.SelectNodes("bpmn:intermediateThrowEvent", $nsManager)
        foreach ($te in $throwEvents) {
            $teId = $te.id
            $teName = $te.GetAttribute("name")
            $nameAttr = if ($teName) { " name=`"$teName`"" } else { "" }
            [void]$sb.AppendLine("    <bpmn2:intermediateThrowEvent id=`"$teId`"$nameAttr>")
            foreach ($inc in $te.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            [void]$sb.AppendLine("    </bpmn2:intermediateThrowEvent>")
            [void]$sb.AppendLine('')
        }
        
        # Intermediate catch events
        $catchEvents = $proc.SelectNodes("bpmn:intermediateCatchEvent", $nsManager)
        foreach ($ce in $catchEvents) {
            $ceId = $ce.id
            $ceName = $ce.GetAttribute("name")
            $nameAttr = if ($ceName) { " name=`"$ceName`"" } else { "" }
            [void]$sb.AppendLine("    <bpmn2:intermediateCatchEvent id=`"$ceId`"$nameAttr>")
            foreach ($inc in $ce.SelectNodes("bpmn:incoming", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:incoming>$($inc.InnerText)</bpmn2:incoming>")
            }
            foreach ($outg in $ce.SelectNodes("bpmn:outgoing", $nsManager)) {
                [void]$sb.AppendLine("      <bpmn2:outgoing>$($outg.InnerText)</bpmn2:outgoing>")
            }
            # Message/timer/signal event definitions
            $msgDef = $ce.SelectSingleNode("bpmn:messageEventDefinition", $nsManager)
            if ($msgDef) {
                [void]$sb.AppendLine("      <bpmn2:messageEventDefinition />")
            }
            $timerDef = $ce.SelectSingleNode("bpmn:timerEventDefinition", $nsManager)
            if ($timerDef) {
                [void]$sb.AppendLine("      <bpmn2:timerEventDefinition />")
            }
            [void]$sb.AppendLine("    </bpmn2:intermediateCatchEvent>")
            [void]$sb.AppendLine('')
        }
        
        # Sequence flows
        $seqFlows = $proc.SelectNodes("bpmn:sequenceFlow", $nsManager)
        foreach ($sf in $seqFlows) {
            $sfId = $sf.id
            $sfName = [System.Security.SecurityElement]::Escape($sf.GetAttribute("name"))
            $sfSource = $sf.GetAttribute("sourceRef")
            $sfTarget = $sf.GetAttribute("targetRef")
            $nameAttr = if ($sfName) { " name=`"$sfName`"" } else { "" }
            
            $condExpr = $sf.SelectSingleNode("bpmn:conditionExpression", $nsManager)
            
            if ($condExpr) {
                [void]$sb.AppendLine("    <bpmn2:sequenceFlow id=`"$sfId`"$nameAttr sourceRef=`"$sfSource`" targetRef=`"$sfTarget`">")
                $condText = $condExpr.InnerText.Trim()
                # Convert FEEL expression to MVEL
                if ($condText.StartsWith("=")) {
                    $condText = $condText.Substring(1).Trim()
                }
                # Convert FEEL equality to Java/MVEL: approved = true -> approved == true
                $condText = $condText -replace '\b(\w+)\s*=\s*(true|false)\b', '$1 == $2'
                $condText = $condText -replace '\b(\w+)\s*=\s*"([^"]*)"', '$1 == "$2"'
                
                $escapedCond = [System.Security.SecurityElement]::Escape($condText)
                [void]$sb.AppendLine("      <bpmn2:conditionExpression xsi:type=`"bpmn2:tFormalExpression`">#{$escapedCond}</bpmn2:conditionExpression>")
                [void]$sb.AppendLine("    </bpmn2:sequenceFlow>")
            } else {
                [void]$sb.AppendLine("    <bpmn2:sequenceFlow id=`"$sfId`"$nameAttr sourceRef=`"$sfSource`" targetRef=`"$sfTarget`" />")
            }
        }
        [void]$sb.AppendLine('')
        
        [void]$sb.AppendLine("  </bpmn2:process>")
        [void]$sb.AppendLine('')
    }
    
    # ---- Diagram section ----
    $diagrams = $xml.SelectNodes("//bpmndi:BPMNDiagram", $nsManager)
    foreach ($diagram in $diagrams) {
        $diagId = $diagram.id
        [void]$sb.AppendLine("  <bpmndi:BPMNDiagram id=`"$diagId`">")
        
        $planes = $diagram.SelectNodes("bpmndi:BPMNPlane", $nsManager)
        foreach ($plane in $planes) {
            $planeId = $plane.id
            $planeBpmnElement = $plane.GetAttribute("bpmnElement")
            [void]$sb.AppendLine("    <bpmndi:BPMNPlane id=`"$planeId`" bpmnElement=`"$planeBpmnElement`">")
            
            # Shapes
            $shapes = $plane.SelectNodes("bpmndi:BPMNShape", $nsManager)
            foreach ($shape in $shapes) {
                $shapeId = $shape.id
                $shapeBpmnElement = $shape.GetAttribute("bpmnElement")
                $isMarkerVisible = $shape.GetAttribute("isMarkerVisible")
                $markerAttr = if ($isMarkerVisible) { " isMarkerVisible=`"$isMarkerVisible`"" } else { "" }
                
                $bounds = $shape.SelectSingleNode("dc:Bounds", $nsManager)
                $bx = $bounds.GetAttribute("x")
                $by = $bounds.GetAttribute("y")
                $bw = $bounds.GetAttribute("width")
                $bh = $bounds.GetAttribute("height")
                
                $label = $shape.SelectSingleNode("bpmndi:BPMNLabel", $nsManager)
                if ($label) {
                    [void]$sb.AppendLine("      <bpmndi:BPMNShape id=`"$shapeId`" bpmnElement=`"$shapeBpmnElement`"$markerAttr>")
                    [void]$sb.AppendLine("        <dc:Bounds x=`"$bx`" y=`"$by`" width=`"$bw`" height=`"$bh`" />")
                    $labelBounds = $label.SelectSingleNode("dc:Bounds", $nsManager)
                    if ($labelBounds) {
                        $lbx = $labelBounds.GetAttribute("x")
                        $lby = $labelBounds.GetAttribute("y")
                        $lbw = $labelBounds.GetAttribute("width")
                        $lbh = $labelBounds.GetAttribute("height")
                        [void]$sb.AppendLine("        <bpmndi:BPMNLabel>")
                        [void]$sb.AppendLine("          <dc:Bounds x=`"$lbx`" y=`"$lby`" width=`"$lbw`" height=`"$lbh`" />")
                        [void]$sb.AppendLine("        </bpmndi:BPMNLabel>")
                    } else {
                        [void]$sb.AppendLine("        <bpmndi:BPMNLabel />")
                    }
                    [void]$sb.AppendLine("      </bpmndi:BPMNShape>")
                } else {
                    [void]$sb.AppendLine("      <bpmndi:BPMNShape id=`"$shapeId`" bpmnElement=`"$shapeBpmnElement`"$markerAttr>")
                    [void]$sb.AppendLine("        <dc:Bounds x=`"$bx`" y=`"$by`" width=`"$bw`" height=`"$bh`" />")
                    [void]$sb.AppendLine("      </bpmndi:BPMNShape>")
                }
            }
            
            # Edges
            $edges = $plane.SelectNodes("bpmndi:BPMNEdge", $nsManager)
            foreach ($edge in $edges) {
                $edgeId = $edge.id
                $edgeBpmnElement = $edge.GetAttribute("bpmnElement")
                [void]$sb.AppendLine("      <bpmndi:BPMNEdge id=`"$edgeId`" bpmnElement=`"$edgeBpmnElement`">")
                
                $waypoints = $edge.SelectNodes("di:waypoint", $nsManager)
                foreach ($wp in $waypoints) {
                    $wpx = $wp.GetAttribute("x")
                    $wpy = $wp.GetAttribute("y")
                    [void]$sb.AppendLine("        <di:waypoint x=`"$wpx`" y=`"$wpy`" />")
                }
                
                $edgeLabel = $edge.SelectSingleNode("bpmndi:BPMNLabel", $nsManager)
                if ($edgeLabel) {
                    $elBounds = $edgeLabel.SelectSingleNode("dc:Bounds", $nsManager)
                    if ($elBounds) {
                        $elbx = $elBounds.GetAttribute("x")
                        $elby = $elBounds.GetAttribute("y")
                        $elbw = $elBounds.GetAttribute("width")
                        $elbh = $elBounds.GetAttribute("height")
                        [void]$sb.AppendLine("        <bpmndi:BPMNLabel>")
                        [void]$sb.AppendLine("          <dc:Bounds x=`"$elbx`" y=`"$elby`" width=`"$elbw`" height=`"$elbh`" />")
                        [void]$sb.AppendLine("        </bpmndi:BPMNLabel>")
                    }
                }
                
                [void]$sb.AppendLine("      </bpmndi:BPMNEdge>")
            }
            
            [void]$sb.AppendLine("    </bpmndi:BPMNPlane>")
        }
        
        [void]$sb.AppendLine("  </bpmndi:BPMNDiagram>")
    }
    
    [void]$sb.AppendLine('')
    [void]$sb.AppendLine('</bpmn2:definitions>')
    
    # Write the converted file
    $outputContent = $sb.ToString()
    [System.IO.File]::WriteAllText($file.FullName, $outputContent, [System.Text.UTF8Encoding]::new($false))
    
    Write-Host "  -> Converted successfully" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Conversion complete! $($files.Count) files converted." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
