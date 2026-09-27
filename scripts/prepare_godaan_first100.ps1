$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$outDir = Join-Path $PSScriptRoot '..\content\godaan-first-100'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
Add-Type -AssemblyName System.Net.Http
$client = [System.Net.Http.HttpClient]::new()
$client.Timeout = [TimeSpan]::FromSeconds(45)
$client.DefaultRequestHeaders.UserAgent.ParseAdd('SahityaReaderPersonal/1.0 (personal offline reading; contact: local project)')
$pages = 3..102
$records = @()
for ($offset = 0; $offset -lt $pages.Count; $offset += 50) {
    $batch = @($pages | Select-Object -Skip $offset -First 50)
    $hindiDigits = @('०','१','२','३','४','५','६','७','८','९')
    $titleMap = @{}
    $titles = foreach ($scanPage in $batch) {
        $pageDigits = -join (([string]$scanPage).ToCharArray() | ForEach-Object { $hindiDigits[[int]::Parse([string]$_)] })
        $title = "पृष्ठ:गो-दान.djvu/$pageDigits"
        $titleMap[$title] = $scanPage
        $title
    }
    $form = [System.Collections.Generic.Dictionary[string,string]]::new()
    $form['action'] = 'query'; $form['format'] = 'json'; $form['prop'] = 'revisions'
    $form['rvprop'] = 'content|ids'; $form['rvslots'] = 'main'; $form['titles'] = ($titles -join '|')
    $content = [System.Net.Http.FormUrlEncodedContent]::new($form)
    $url = 'https://hi.wikisource.org/w/api.php'
    $response = $client.PostAsync($url, $content).GetAwaiter().GetResult().Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $data = $response | ConvertFrom-Json
    if ($data.error) { throw "Wikisource API error: $($data.error.info)" }
    foreach ($page in $data.query.pages.PSObject.Properties.Value) {
        $title = $page.title
        if (-not $titleMap.ContainsKey($title) -or -not $page.revisions) { throw "Missing transcribed page $title" }
        $scanPage = $titleMap[$title]
        $revision = $page.revisions[0]
        $wiki = $revision.slots.main.'*'
        $quality = [regex]::Match($wiki, '<pagequality\s+level="(\d)"')
        $runningHead = [regex]::Match($wiki, '\{\{rh\|([^}]*)\}\}')
        $body = [regex]::Replace($wiki, '(?is)<noinclude>.*?</noinclude>', '')
        $body = [regex]::Replace($body, '(?is)<!--.*?-->', '')
        $body = [regex]::Replace($body, '(?is)\{\{(?:c|center|small|larger|sc)\|([^{}]*)\}\}', '$1')
        $body = [regex]::Replace($body, '(?is)\{\{right\|[^{}]*\}\}', '')
        for ($pass = 0; $pass -lt 8 -and $body -match '\{\{[^{}]*\}\}'; $pass++) {
            $body = [regex]::Replace($body, '(?s)\{\{[^{}]*\}\}', '')
        }
        $body = [regex]::Replace($body, '(?i)<br\s*/?>', "`n")
        $body = [regex]::Replace($body, '(?is)<[^>]+>', '')
        $body = [regex]::Replace($body, '\[\[([^\]|]+)\|([^\]]+)\]\]', '$2')
        $body = [regex]::Replace($body, '\[\[([^\]]+)\]\]', '$1')
        $body = $body -replace "'''|''", ''
        $body = [regex]::Replace($body, '(?m)^\s*\*.*$', '')
        $body = [regex]::Replace($body, '(?i)\{+\s*gap[\}\)\]]*', '')
        $body = $body -replace '[{}]', ''
        $body = [regex]::Replace($body, "[ \t]+", ' ')
        $body = [regex]::Replace($body, "\n\s*\n(?:\s*\n)+", "`n`n").Trim()
        $printPage = $scanPage - 2
        $folioDigits = $null
        if ($runningHead.Success) {
            $headFields = $runningHead.Groups[1].Value -replace "'''", ''
            $folioField = ($headFields -split '\|' | Where-Object { $_ -match '^[०-९0-9]+$' } | Select-Object -First 1)
            if ($folioField) {
                $folioDigits = -join ($folioField.ToCharArray() | ForEach-Object {
                    $digit = [string]$_; $index = [Array]::IndexOf(@('०','१','२','३','४','५','६','७','८','९'), $digit)
                    if ($index -ge 0) { [string]$index } else { $digit }
                })
            }
        }
        $scanDigits = -join (([string]$scanPage).ToCharArray() | ForEach-Object { $hindiDigits[[int]::Parse([string]$_)] })
        $records += [pscustomobject]@{
            sequencePage = $scanPage - 2
            sourceFolioTemplate = $folioDigits
            scanPage = $scanPage
            proofreadQuality = if ($quality.Success) { [int]$quality.Groups[1].Value } else { 0 }
            revisionId = [long]$revision.revid
            blankScan = [string]::IsNullOrWhiteSpace($body)
            sourcePageUrl = "https://hi.wikisource.org/wiki/" + [uri]::EscapeDataString("पृष्ठ:गो-दान.djvu/$scanDigits")
            text = $body
        }
    }
    Write-Progress -Activity 'Preparing Godaan pages' -Status "Fetched $([Math]::Min($offset + 50, 100)) of 100 pages" -PercentComplete ([int](100 * [Math]::Min($offset + 50, 100) / 100))
}
$client.Dispose()
$records = @($records | Sort-Object sequencePage)

if ($records.Count -ne 100) { throw "Expected 100 page records, received $($records.Count)" }
foreach ($r in $records) {
    if ($r.sequencePage -lt 1 -or $r.sequencePage -gt 100 -or (-not $r.blankScan -and $r.proofreadQuality -lt 3)) {
        throw "Page validation failed: sequence=$($r.sequencePage), scan=$($r.scanPage), quality=$($r.proofreadQuality)"
    }
}

$txt = [System.Text.StringBuilder]::new()
[void]$txt.AppendLine('गोदान — पहले 100 आंतरिक स्कैन-पृष्ठ (हिंदी)')
[void]$txt.AppendLine('स्कैन छवियाँ 3–102; शुरुआती दो आवरण-पृष्ठ शामिल नहीं हैं।')
[void]$txt.AppendLine('प्रेमचंद | सरस्वती प्रेस, बनारस, 1936 संस्करण')
[void]$txt.AppendLine('स्रोत-पृष्ठ क्रमांक मुद्रित पृष्ठ संख्या नहीं है; छपी संख्या और जाँच-विवरण sources-and-qc.md में देखें।')
foreach ($r in $records) {
    [void]$txt.AppendLine()
    [void]$txt.AppendLine("━━ स्कैन पृष्ठ $($r.scanPage) ━━")
    if ($r.blankScan) { [void]$txt.AppendLine('[इस स्कैन पृष्ठ में पठनीय पाठ नहीं है।]') }
    else { [void]$txt.AppendLine($r.text) }
}
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText((Join-Path $outDir 'godaan-first-100-pages-hi.txt'), $txt.ToString(), $utf8NoBom)
$records | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $outDir 'godaan-first-100-pages-hi.json')
$records | Select-Object sequencePage,scanPage,sourceFolioTemplate,proofreadQuality,revisionId,@{n='characters';e={$_.text.Length}} | Export-Csv -NoTypeInformation -Encoding utf8 (Join-Path $outDir 'page-check.csv')
Write-Output "Prepared $($records.Count) pages; quality levels: $((($records | Group-Object proofreadQuality | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ', '))"
Write-Output "Text characters: $($records.text | ForEach-Object Length | Measure-Object -Sum | Select-Object -ExpandProperty Sum)"
