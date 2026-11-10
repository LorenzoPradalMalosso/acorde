# Acorde — Brand Kit oficial do projeto

**Versão:** 1.0 | **Identidade:** Acorde | **Slogan:** Gestão em sintonia

> Identidade visual aprovada para o projeto acadêmico Acorde. Este kit documenta a identidade do produto; não representa registro de marca no INPI nem confirmação da disponibilidade do nome.

## 1. Conceito

**Acorde** expressa harmonia entre **pessoas, processos e impacto positivo**. A proposta conecta gestão operacional, tarefas, colaboração e micro-ESG em uma experiência corporativa clara, contemporânea e acessível.

### Personalidade da marca

- Profissional, confiável, contemporânea e acessível.
- Sofisticação com simplicidade, nunca excesso de efeitos.
- Sustentabilidade representada de forma abstrata, sem depender de símbolos literais de folhas.

## 2. Construção do símbolo

O monograma é um **A assimétrico, construído a partir de duas faixas independentes**: a faixa superior desenha a ascensão e o início da haste direita; a inferior estabelece a conexão horizontal e o fechamento da forma. O espaço negativo entre as faixas reforça fluxo e convergência.

**Versão principal:** faixa superior em Verde Profundo e faixa inferior em Menta. Em fundos escuros, faixa superior branca e faixa inferior menta.

**Atenção:** o símbolo vetorial deste pacote é uma reconstrução cuidadosa da direção visual aprovada, padronizada para uso digital. Para alterar sua geometria, revisar integralmente favicon, app icons e versões monocromáticas.

## 3. Identidade cromática

| Token | Hex | Uso recomendado |
|---|---|---|
| Verde profundo | `#0F3D34` | Cor institucional; logo; superfícies escuras; CTAs |
| Verde médio | `#1F6B58` | Acentos secundários e visuais de apoio |
| Menta | `#7FD8B0` | Destaque do logo, pontos de atenção positiva e detalhes |
| Verde claro | `#E8F6F0` | Superfícies e fundos suaves |
| Off-white | `#F7FAF8` | Fundo claro principal |
| Grafite | `#1F2A28` | Texto principal em fundo claro |
| Cinza | `#66746D` | Textos auxiliares |
| Borda | `#DDEAE4` | Bordas e divisões sutis |

**Acessibilidade:** não use menta como cor de texto sobre branco sem verificar contraste. Não confunda tokens de marca com cores funcionais de erro, alerta e sucesso; essas precisam de verificação própria.

## 4. Tipografia

- **Marca e títulos:** Manrope (preferencial) em pesos Semibold/Bold.
- **Interface e textos:** Inter Regular, Medium ou Semibold.
- **Wordmark no SVG:** texto convertido em vetores, baseado em Inter Display ExtraBold para garantir aparência consistente sem depender de fontes instaladas. O logotipo aprovado é um desenho fixo; não deve ser substituído por texto digitado em outra fonte.
- **Fontes:** instale as fontes por canais oficiais e siga suas licenças; arquivos de fonte não estão incluídos no pacote.

## 5. Versões de logotipo

| Situação | Arquivo recomendado |
|---|---|
| Documentos e interfaces claras | `logos/svg/acorde-horizontal-principal.svg` |
| Fundo verde profundo/escuro | `logos/svg/acorde-horizontal-inverso.svg` |
| Impressão com tinta única escura | `logos/svg/acorde-horizontal-monocromatico-escuro.svg` |
| Impressão ou gravação monocromática clara | `logos/svg/acorde-horizontal-monocromatico-claro.svg` |
| Espaço vertical | `logos/svg/acorde-vertical-principal.svg` |
| Favicon ou menu compacto | `icones/svg/acorde-favicon.svg` |
| Loja de aplicativos | `icones/png/acorde-app-escuro-1024.png` |

Todas as versões em `logos/png/` possuem **fundo transparente**. O símbolo isolado tem área transparente ao redor para evitar cortes.

## 6. Área de proteção e tamanhos

- Área mínima livre de outros elementos: **aproximadamente 1/4 da altura visível do monograma** em cada lado.
- Logo horizontal: recomendado **180 px ou mais** de largura em interfaces.
- Monograma isolado: recomendado **24 px ou mais** em interface; para 16 px, use favicon próprio.
- Não aplicar sombra, esticar, contornar ou girar o logotipo.
- Não recolorir arbitrariamente as faixas: usar as variantes oficiais.
- Para impressão e placas maiores, preferir SVG e testar a reprodução em tamanho real.

## 7. Ícones e aplicações digitais

- Ícone principal de aplicativo: fundo Verde Profundo, monograma branco e menta.
- Ícone alternativo claro: fundo Off-white, monograma Verde Profundo e Menta.
- Android adaptativo: arquivos separados `android-adaptive-foreground` e `android-adaptive-background`.
- Web: `favicon.ico`, SVG de favicon e PNGs 16/32/48/64.
- Banner para GitHub: `social/acorde-github-banner-1280x440.png`.

## 8. Voz e assinatura

**Nome no texto:** Acorde. **Marca gráfica:** `acorde.` em minúsculas com ponto final verde/menta.

**Slogan:** *Gestão em sintonia.*

Exemplos de tom: “Organize suas tarefas.”, “Acompanhe os resultados da equipe.”, “Transforme pequenas ações em impacto positivo.” Evite promessas de redução ambiental medidas quando o valor for somente estimado.

## 9. Uso no código

- React/Vite: importar `tokens/acorde.css`, utilizar os SVGs em `public/branding/` e definir fontes em CSS.
- Flutter: incorporar `tokens/acorde_colors.dart`; incluir ícones em `assets/branding/`, cadastrar fontes no `pubspec.yaml`.
- Não codificar múltiplas tonalidades de verde em telas isoladas; referenciar tokens da marca.

## 10. Checklist de validação

- [ ] Logo horizontal e ícone legíveis em 24/32 px.
- [ ] Contrastes de texto e botões revisados conforme WCAG.
- [ ] SVGs consistentes nos navegadores e PNGs com fundo transparente.
- [ ] Identidade aplicada de forma uniforme em Web, Mobile, README e apresentação.
- [ ] Nome e identidade verificados quanto a uso de terceiros antes de qualquer lançamento comercial.
