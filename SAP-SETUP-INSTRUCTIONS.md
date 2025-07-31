# Instruções de Configuração SAP - FlowDeconstruct

## Configuração da JVM da SAP (SAP JVM 8)

Este projeto foi configurado especificamente para usar **SAP JVM 8** existente, a JVM otimizada da SAP.

## Por que SAP JVM 8?

**SAP JVM 8 é a máquina virtual Java otimizada da SAP**, projetada especificamente para aplicações empresariais SAP e oferece performance superior em ambientes corporativos.

### Benefícios do SAP JVM 8:

- **Otimização Enterprise**: Otimizada especificamente para cargas de trabalho SAP
- **Performance Superior**: Melhor desempenho em aplicações empresariais
- **Recursos Avançados**: Inclui recursos enterprise específicos da SAP
- **Compatibilidade Java 8**: 100% compatível com padrões Java 8
- **Suporte SAP**: Suporte oficial da SAP para ambientes empresariais

## Configuração Automática (Recomendada)

### Passo 1: Executar o Script de Configuração

```powershell
# Abra PowerShell como Administrador
# Navegue até o diretório do projeto
cd C:\fabio\programas\MCC\FlowMapper

# Execute o script de configuração
.\setup-environment.ps1
```

### O que o script faz:

1. **Verifica SAP JVM 8** existente em `C:\Program Files\sapjvm\sapjvm_8`
2. **Instala Chocolatey** (se não estiver instalado)
3. **Instala Apache Maven**
4. **Configura JAVA_HOME** para apontar para SAP JVM 8
5. **Verifica a instalação**

## Configuração Manual (Alternativa)

### Passo 1: Verificar SAP JVM 8

1. Confirme que o SAP JVM 8 está instalado em: `C:\Program Files\sapjvm\sapjvm_8`
2. Verifique se o diretório `bin` existe e contém `java.exe` e `javac.exe`
3. Teste a instalação: `"C:\Program Files\sapjvm\sapjvm_8\bin\java.exe" -version`

### Passo 2: Configurar Variáveis de Ambiente

```powershell
# Definir JAVA_HOME
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\sapjvm\sapjvm_8", "Machine")

# Adicionar ao PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
[Environment]::SetEnvironmentVariable("Path", "$currentPath;C:\Program Files\sapjvm\sapjvm_8\bin", "Machine")
```

### Passo 3: Instalar Maven

1. Baixe Maven de: https://maven.apache.org/download.cgi
2. Extraia para `C:\Program Files\Apache\Maven`
3. Adicione `C:\Program Files\Apache\Maven\bin` ao PATH

## Verificação da Instalação

```powershell
# Verificar Java (SAP JVM 8)
java -version
# Deve mostrar: java version "1.8.0_xxx" ... SAP Java Server VM

# Verificar Maven
mvn -version
# Deve mostrar: Apache Maven 3.x.x

# Verificar JAVA_HOME
echo $env:JAVA_HOME
# Deve mostrar: C:\Program Files\sapjvm\sapjvm_8
```

## Configuração Maven para SAP JVM 8

O projeto já está configurado no `pom.xml` com:

```xml
<properties>
    <!-- SAP JVM 8 Configuration -->
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <java.version>1.8</java.version>
    <sapjvm.home>C:\Program Files\sapjvm\sapjvm_8</sapjvm.home>
</properties>
```

A configuração `maven.compiler.source` e `maven.compiler.target` garante compatibilidade com Java 8.

## Compilação e Execução

Após a configuração:

```powershell
# Compilar o projeto
mvn clean compile

# Executar a aplicação
mvn exec:java -Dexec.mainClass="com.sap.flowdeconstruct.FlowDeconstructApp"

# Criar JAR distribuível
mvn clean package
java -jar target/FlowDeconstruct.jar
```

## Compatibilidade SAP

### Versões Suportadas

**SAP JVM 8** é a versão enterprise da JVM otimizada para aplicações SAP, oferecendo recursos avançados e performance superior.

- **SAP JVM 8**: Versão enterprise com otimizações SAP
- **Compatibilidade Java 8**: 100% compatível com Java SE 8
- **Recursos Enterprise**: Inclui funcionalidades avançadas para ambientes corporativos

### Integração com Ferramentas SAP

SAP JVM 8 oferece integração nativa com o ecossistema SAP, proporcionando melhor performance e compatibilidade com aplicações empresariais.

## Solução de Problemas

### Problema: "mvn não é reconhecido"
**Solução**: Reinicie o terminal após a instalação ou execute `refreshenv`

### Problema: "JAVA_HOME não está definido"
**Solução**: Execute o script de configuração como Administrador

### Problema: "Versão Java incorreta"
**Solução**: Verifique se JAVA_HOME aponta para SAP JVM 8:
```powershell
echo $env:JAVA_HOME
# Deve ser: C:\Program Files\sapjvm\sapjvm_8
```

## Próximos Passos

1. ✅ **Configurar SAP JVM 8 e Maven** (este documento)
2. ⏭️ **Compilar o projeto**: `mvn clean compile`
3. ⏭️ **Executar a aplicação**: `mvn exec:java`
4. ⏭️ **Criar JAR distribuível**: `mvn clean package`

---

**Nota**: Este projeto foi especificamente configurado para ambientes SAP e utiliza SAP JVM 8 para garantir máxima compatibilidade e performance empresarial.