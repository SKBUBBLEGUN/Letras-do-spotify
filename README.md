# Letras do Spotify

Aplicação web/PWA para mostrar letras sincronizadas da música reproduzida no Spotify, com controles básicos do player.

## Publicar no GitHub Pages

1. Abra **Settings → Pages** neste repositório.
2. Em **Build and deployment**, escolha **Deploy from a branch**.
3. Selecione a branch **main** e a pasta **/(root)** e salve.
4. Aguarde o deploy em **Actions**. O endereço será `https://skbubblegun.github.io/Letras-do-spotify/`.

## Configurar o Spotify

1. Crie um aplicativo em [Spotify for Developers](https://developer.spotify.com/dashboard).
2. Adicione como **Redirect URI** exatamente o endereço publicado acima, incluindo a barra final.
3. Ative **Web API**, salve e copie o **Client ID**.
4. Abra o site, cole o Client ID e conecte sua conta Spotify.

O login usa Authorization Code com PKCE; Client ID e tokens são guardados no armazenamento local do navegador. Não coloque Client Secret neste projeto.

**Observações:** os controles de reprodução podem exigir Spotify Premium e um dispositivo Spotify ativo. As letras são consultadas no LRCLIB e podem não existir para todas as faixas. O Spotify pode alterar requisitos e permissões da Web API.
