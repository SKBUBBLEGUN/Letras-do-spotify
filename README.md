# Letras do Spotify

Aplicação Android e web para exibir letras da música reproduzida no Spotify, com controles do player.

## Gerar e baixar o APK

1. Abra **Actions → Build Letras do Spotify APK**.
2. Entre na execução mais recente concluída com sucesso.
3. Em **Artifacts**, baixe **Letras-do-Spotify-APK**.
4. Extraia o ZIP e instale `app-debug.apk` no Android. Se solicitado, permita a instalação desta fonte.

O APK é uma versão Android que abre a aplicação hospedada e mantém os dados do WebView no aparelho. O login Spotify é aberto no navegador externo e retorna ao aplicativo.

## Publicar a versão web (necessário para o APK)

1. Abra **Settings → Pages** neste repositório.
2. Em **Build and deployment**, escolha **Deploy from a branch**.
3. Selecione a branch **main** e a pasta **/(root)** e salve.
4. O endereço será `https://skbubblegun.github.io/Letras-do-spotify/`.

## Configurar o Spotify

1. Crie um aplicativo em [Spotify for Developers](https://developer.spotify.com/dashboard).
2. Adicione como **Redirect URI** exatamente `https://skbubblegun.github.io/Letras-do-spotify/` (incluindo a barra final).
3. Ative **Web API**, salve e copie o **Client ID**.
4. Abra o APK, cole o Client ID e conecte sua conta Spotify.

O login usa Authorization Code com PKCE; Client ID e tokens ficam no armazenamento local do aplicativo. Não coloque Client Secret neste projeto.

**Limitações:** os controles de reprodução podem exigir Spotify Premium e um dispositivo Spotify ativo. As letras são consultadas no LRCLIB e podem não existir para todas as faixas. O pop-up flutuante sobre outros aplicativos não é disponibilizado pelo wrapper Android nesta primeira versão.
