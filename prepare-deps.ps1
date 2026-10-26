param([Parameter(Mandatory = $true)][string]$ModsPath, [switch]$IncludeJei)
$ErrorActionPreference = 'Stop'
$dependencies = @(
    @{ Source = 'ldlib-forge-1.20.1-1.0.50.jar'; Target = 'ldlib-forge-1.20.1-1.0.50.jar'; Hash = '559D2A0CF8F77D7D6487B2C1620A8EE09E6BED94268BE6DF77CBA0F7D8AC5112' },
    @{ Source = 'emi-1.1.24+1.20.1+forge.jar'; Target = 'emi-forge-1.20.1-1.1.24.jar'; Hash = '92784DCF5222D72983A420C90F2E9240468EE0FECE96F757C82D7884B6CBDB72' },
    @{ Source = 'Mekanism-1.20.1-10.4.16.80.jar'; Target = 'mekanism-1.20.1-10.4.16.80.jar'; Hash = '3B0D191FA4A45E662F9725991D76A30192F30186ABB2BA694B873B5F7B9358D2' },
    @{ Source = 'MekanismGenerators-1.20.1-10.4.16.80.jar'; Target = 'mekanism-generators-1.20.1-10.4.16.80.jar'; Hash = 'BE2D5B8553F8638CFABF491002F95CEF6ACEF210F627ADDB391F5AF713502512' },
    @{ Source = 'JustEnoughMekanismMultiblocks-1.20.1-4.27.jar'; Target = 'jemm-1.20.1-4.27.jar'; Hash = 'D4B228FB6DA4E6B75DAE68719B1B3FD3926561B6394E2ECC0643E93895D6B5B3' },
    @{ Source = 'toomanyrecipeviewers-0.9.0+mc.20.1.jar'; Target = 'tmrv-forge-1.20.1-0.9.0.jar'; Hash = '6B303725304189CA2A57DF34727C43E4960EE3758AD88C1B0C0486A3775754E7' }
)
if ($IncludeJei) {
    $dependencies += @{ Source = 'jei-1.20.1-forge-15.20.0.132.jar'; Target = 'jei-1.20.1-forge-15.20.0.132.jar'; Hash = 'D9F60B5C16E778D7A27A3DDDDA0B8D3B693CFBA6CF1B48EF4BE3D8471B83FED9' }
}
# Validate all sources before changing the local dependency directory.
foreach ($dependency in $dependencies) {
    $source = Join-Path $ModsPath $dependency.Source
    if ((Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $dependency.Hash) {
        throw "Dependency does not match the tested artifact: $($dependency.Source)"
    }
}
$dependencyDirectory = Join-Path $PSScriptRoot 'deps'
New-Item -ItemType Directory -Path $dependencyDirectory -Force | Out-Null
foreach ($dependency in $dependencies) {
    Copy-Item -LiteralPath (Join-Path $ModsPath $dependency.Source) -Destination (Join-Path $dependencyDirectory $dependency.Target) -Force
}
Write-Output "Prepared $($dependencies.Count) pinned development dependencies."
