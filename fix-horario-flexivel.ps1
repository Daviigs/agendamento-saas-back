# Script de Correção Automática - Horário Flexível
# Execute este script para corrigir o problema da coluna horario_flexivel

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Correção: Horário Flexível V4    " -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Configurações do banco
$DB_HOST = "localhost"
$DB_PORT = "5432"
$DB_NAME = "agendamentodb"
$DB_USER = "postgres"
$DB_PASSWORD = "postgress"

Write-Host "🔍 Verificando estado atual..." -ForegroundColor Yellow
Write-Host ""

# SQL para verificar e corrigir
$SQL_FIX = @"
-- Verificar estado atual
SELECT
    'Estado Atual da Coluna' as info,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'tb_tenant_working_hours'
  AND column_name = 'horario_flexivel';

-- Remover coluna problemática
ALTER TABLE tb_tenant_working_hours
DROP COLUMN IF EXISTS horario_flexivel;

-- Remover migration do histórico (se existir)
DELETE FROM flyway_schema_history WHERE version = '4';

-- Confirmar remoção
SELECT 'Coluna Removida - Pronta para Reaplicar Migration' as status;
"@

Write-Host "📝 SQL que será executado:" -ForegroundColor Yellow
Write-Host $SQL_FIX -ForegroundColor Gray
Write-Host ""

$continuar = Read-Host "Deseja executar a correção? (S/N)"

if ($continuar -eq "S" -or $continuar -eq "s") {
    Write-Host ""
    Write-Host "⚙️  Executando correção no banco de dados..." -ForegroundColor Yellow

    # Salvar SQL em arquivo temporário
    $tempFile = [System.IO.Path]::GetTempFileName()
    $SQL_FIX | Out-File -FilePath $tempFile -Encoding UTF8

    # Executar usando psql
    $env:PGPASSWORD = $DB_PASSWORD
    $output = & psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $tempFile 2>&1

    # Remover arquivo temporário
    Remove-Item $tempFile

    Write-Host ""
    Write-Host "📊 Resultado:" -ForegroundColor Cyan
    Write-Host $output
    Write-Host ""

    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Correção executada com sucesso!" -ForegroundColor Green
        Write-Host ""
        Write-Host "🚀 Próximos passos:" -ForegroundColor Yellow
        Write-Host "   1. Reinicie a aplicação: .\mvnw.cmd spring-boot:run" -ForegroundColor White
        Write-Host "   2. O Flyway aplicará automaticamente a migration V4 corrigida" -ForegroundColor White
        Write-Host "   3. Verifique os logs para confirmar sucesso" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host "❌ Erro ao executar correção!" -ForegroundColor Red
        Write-Host "   Verifique se:" -ForegroundColor Yellow
        Write-Host "   - PostgreSQL está rodando" -ForegroundColor White
        Write-Host "   - Credenciais estão corretas" -ForegroundColor White
        Write-Host "   - Banco de dados existe" -ForegroundColor White
        Write-Host ""
    }
} else {
    Write-Host ""
    Write-Host "❌ Operação cancelada pelo usuário" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "📖 Você pode executar manualmente os comandos SQL em:" -ForegroundColor Cyan
    Write-Host "   FIX_HORARIO_FLEXIVEL.sql" -ForegroundColor White
    Write-Host ""
}

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "   Documentação Completa:           " -ForegroundColor Cyan
Write-Host "   SOLUCAO_ERRO_HORARIO_FLEXIVEL.md " -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Pausa no final
Read-Host "Pressione ENTER para sair"

