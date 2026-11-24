# Letras do Spotify — Android nativo

O APK é um aplicativo Android nativo: a interface, a autenticação e a lógica ficam dentro do próprio aplicativo. **Não abre o GitHub Pages nem carrega a interface de um site.** A conexão com Spotify e LRCLIB usa internet.

## Gerar o APK

1. Abra **Actions → Build Letras do Spotify APK**.
2. Aguarde a execução terminar com status **Success**.
3. Em **Artifacts**, baixe **Letras-do-Spotify-APK**.
4. Extraia o ZIP e instale `app-debug.apk` no Android.

## Configurar o Spotify (uma vez)

1. Crie um aplicativo em [Spotify for Developers](https://developer.spotify.com/dashboard).
2. Nas configurações do app, adicione exatamente este Redirect URI: `letrasspotify://callback`.
3. Copie o **Client ID** (não use Client Secret).
4. Abra o APK, cole o Client ID e toque em **Conectar ao Spotify**.
5. Autorize o acesso no Spotify. O navegador retorna ao aplicativo.

O login usa Authorization Code com PKCE. O Client ID e os tokens são armazenados localmente no aparelho. O APK não contém Client Secret.

## Recursos desta versão

- Interface Android nativa, sem WebView e sem dependência do GitHub Pages.
- Consulta a música em reprodução no Spotify.
- Busca letras sincronizadas ou simples no LRCLIB.
- Destaque e rolagem automática da linha atual.
- Controles de reproduzir/pausar, faixa anterior/próxima e posição da música.

## Observações

- É necessária conexão com a internet para Spotify e LRCLIB.
- Os controles de reprodução podem exigir Spotify Premium e um dispositivo Spotify ativo.
- Nem todas as músicas possuem letras no LRCLIB.
- O acesso à Web API do Spotify depende das permissões e limites aplicáveis à sua conta e ao aplicativo cadastrado.
