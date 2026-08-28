# Compila (se preciso) e roda um dos jogos do Fliperama
# Uso: .\rodar.ps1 <numero-ou-nome-do-jogo>
# Exemplos:
#   .\rodar.ps1 1
#   .\rodar.ps1 pacman
#   .\rodar.ps1 Ge_04_Pacman_tavares

param(
    [Parameter(Mandatory = $true)]
    [string]$Jogo
)

$ErrorActionPreference = "Stop"

$projetos = @{
    "1"       = "Ge_01_Telejogo78"
    "telejogo"= "Ge_01_Telejogo78"
    "2"       = "Ge_02_ Space_BarBiju"
    "space"   = "Ge_02_ Space_BarBiju"
    "invaders"= "Ge_02_ Space_BarBiju"
    "3"       = "Ge_03_Tetris_Otica_Laranjal"
    "tetris"  = "Ge_03_Tetris_Otica_Laranjal"
    "4"       = "Ge_04_Pacman_tavares"
    "pacman"  = "Ge_04_Pacman_tavares"
    "5"       = "Ge_05_Asteroids_bar_zecareca"
    "asteroids"= "Ge_05_Asteroids_bar_zecareca"
    "6"       = "Ge_06_Balsa_Capela Quebrada"
    "balsa"   = "Ge_06_Balsa_Capela Quebrada"
}

$chave = $Jogo.ToLower()
if ($projetos.ContainsKey($chave)) {
    $proj = $projetos[$chave]
} elseif (Test-Path $Jogo) {
    $proj = $Jogo
} else {
    Write-Host "Jogo nao reconhecido: $Jogo" -ForegroundColor Red
    Write-Host "Opcoes: 1/telejogo, 2/space/invaders, 3/tetris, 4/pacman, 5/asteroids, 6/balsa"
    exit 1
}

$src = Join-Path $proj 'src'
$bin = Join-Path $proj 'bin'

if (-not (Test-Path $src)) {
    Write-Host "Projeto nao encontrado: $proj" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $bin)) {
    New-Item -ItemType Directory -Path $bin | Out-Null
}

Write-Host "Compilando $proj..." -ForegroundColor Yellow
$fontes = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d $bin $fontes
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro ao compilar $proj" -ForegroundColor Red
    exit 1
}

Write-Host "Executando $proj..." -ForegroundColor Green
Push-Location $proj
try {
    java -cp bin br.com.mvbos.lgj.Jogo
} finally {
    Pop-Location
}
