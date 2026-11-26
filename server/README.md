# Ativar pesquisa automática 3.0

O aplicativo Android envia somente cidade, bairro, condomínio (opcional), tipo, área, finalidade e características ao servidor HTTPS. Não envia nome de cliente, fotos, endereço cadastrado ou histórico. Não informe dados pessoais no campo condomínio.

## Publicar no Render
1. Crie uma conta em https://render.com/ e conecte o repositório deste aplicativo.
2. Crie um Blueprint usando render.yaml ou um Web Service Node com rootDir server, buildCommand npm test, startCommand npm start.
3. Configure OPENAI_API_KEY exclusivamente nas variáveis secretas do servidor. Crie a chave na conta do projeto em https://platform.openai.com/api-keys e configure faturamento e limites/alertas de gastos no projeto. Nunca cole a chave no APK, chat ou GitHub.
4. Configure APP_TOKEN como um segredo aleatório de pelo menos 24 caracteres (o Blueprint gera um). Use o endereço HTTPS do serviço e APP_TOKEN na tela Configuração do serviço do aplicativo. Não compartilhe esse código. OPENAI_MODEL pode ser alterado no servidor.
5. Abra /health no endereço público para verificar o serviço. Esse teste não consome créditos e não comprova que a pesquisa real funciona.
6. Faça uma consulta real no aplicativo e confira os anúncios antes de incluir. A disponibilidade depende da cobertura web, do modelo e das fontes autorizadas.

O servidor impõe limites em memória: uma consulta simultânea, duas por minuto e vinte por 24 horas. Reiniciar ou replicar o serviço reinicia ou distribui esses limites; para uso comercial, adote autenticação individual e quota persistente. A versão é de testes, não um PTAM e não garante que anúncios estejam ativos. Pesquisas web e hospedagem podem gerar cobranças; confira o plano antes de publicar.

Testes locais: cd server && npm test. Os testes são simulados; a consulta real exige chave e implantação. O APK é assinado com chave de depuração e poderá exigir desinstalação da versão anterior se a assinatura mudar. Exporte o backup antes de desinstalar.
