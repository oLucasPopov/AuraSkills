# Design: Novas skills Trading, Husbandry e Smithing

**Data:** 2026-09-24
**Status:** Implementado (branch feature/fork-skills)
**Repositório de trabalho:** fork `origin` = `https://github.com/oLucasPopov/AuraSkills.git`; upstream `upstream` = `https://github.com/Archy-X/AuraSkills.git`

## 1. Visão geral

Adicionar 3 novas skills completas ao AuraSkills, cobrindo mecânicas vanilla que as 11 skills ativas não cobrem:

| Skill | Mecânica central | Stats de reward | Mana ability |
|-------|------------------|-----------------|--------------|
| **Trading** (Comércio) | Trocas com villagers/wandering traders, bartering com piglins | Luck + Wisdom | `grand_bargain` |
| **Husbandry** (Criação) | Breeding, doma, shear/ordenha de animais | Health + Regeneration | `animal_whisperer` |
| **Smithing** (Ferraria) | Smithing table, fundição, crafting de equipamentos | Toughness + Strength | `forge_overdrive` |

Cada skill segue integralmente a convenção das skills existentes: 5 abilities (a 2ª é a multiplicadora de XP), 1 mana ability, XP sources próprios, stat rewards por nível, entrada no menu `/skills`, comando `/<skill>`, mensagens localizáveis e suporte automático a leaderboards/placeholders/jobs.

**Escopo explicitamente fora:** novas stats (usamos apenas as 9 existentes), mudanças em skills existentes, novos loot tables, traduções para os outros 19 locales (fallback em inglês, padrão do projeto para conteúdo novo).

## 2. Estratégia de merge com o upstream (requisito mandatório)

O código deve ser escrito para que `git merge upstream/master` futuro gere o mínimo de conflitos. Regras:

1. **Todo código novo em arquivos novos.** Classes de abilities, mana abilities, levelers, parsers e testes ficam em arquivos próprios (ex.: `bukkit/.../skills/trading/TradingAbilities.java`). Nenhuma lógica nova é embutida em classes existentes do upstream.
2. **Edições em arquivos do upstream são exclusivamente append-only e delimitadas.** Onde for inevitável tocar um arquivo existente (enums, listas de registro, YAMLs), as adições vão em bloco contíguo no final da seção correspondente, marcadas com um banner de comentário uniforme:
   ```java
   // === Fork skills: trading, husbandry, smithing ===
   ```
   Em YAML, bloco contíguo ao final da seção com comentário `# === Fork skills ===`. Conflitos de merge ficam assim triviais de resolver (aceitar "ours" + "theirs" no mesmo bloco).
3. **Sem refatoração de código do upstream.** Não renomear, reordenar ou "melhorar" nada fora do escopo.
4. **Ordem alfabética/posicional do upstream é preservada** — novos itens vão ao final de listas, nunca intercalados, para reduzir a chance de conflito textual.
5. **Configs novas são arquivos novos** (`sources/trading.yml`, `rewards/trading.yml` etc.). Os únicos YAMLs existentes editados são: `skills.yml` (append de 3 entradas), `abilities.yml` (append), `mana_abilities.yml` (append), `messages_en.yml` (append), `menus/skills.yml` (append de 3 contextos).
6. **Versões de arquivo de config:** `SourceFileUpdates`, `MenuFileUpdates` e afins recebem entradas novas append-only para que servidores existentes recebam os novos arquivos sem reset, seguindo o mecanismo de `file_version` já existente.
7. **Branches:** trabalho em branch de feature no fork (ex.: `feature/fork-skills`), merge para `master` do fork após conclusão. Nunca commitar diretamente sobre histórico do upstream sem branch.

## 3. Arquitetura e convenções (todas as 3 skills)

Artefatos por skill (padrão idêntico às skills existentes):

| Artefato | Local | Tipo |
|----------|-------|------|
| Constante do enum | `api/.../api/skill/Skills.java` | modificado (append) |
| 5 abilities | `api/.../api/skill/Abilities.java` | modificado (append) |
| 1 mana ability | `api/.../api/skill/ManaAbilities.java` | modificado (append) |
| Interfaces de source type novas | `api/.../api/source/type/` | novos arquivos |
| Registro de source types | `common/.../source/SourceTypes.java`, `SourceTypeRegistry.java` | modificado (append) |
| Parsers de source | `common/.../source/parser/` | novos arquivos |
| Definição da skill | `common/src/main/resources/skills.yml` | modificado (append) |
| XP sources | `common/src/main/resources/sources/<skill>.yml` | novo arquivo |
| Stat rewards | `common/src/main/resources/rewards/<skill>.yml` | novo arquivo |
| Balanceamento de abilities | `common/src/main/resources/abilities.yml` | modificado (append) |
| Balanceamento de mana ability | `common/src/main/resources/mana_abilities.yml` | modificado (append) |
| Mensagens | `common/src/main/resources/messages/messages_en.yml` | modificado (append) |
| Listener de abilities | `bukkit/.../skills/<skill>/<Skill>Abilities.java` | novo arquivo |
| Provider de mana ability | `bukkit/.../skills/<skill>/<ManaAbility>.java` | novo arquivo |
| Levelers (listeners de XP) | `bukkit/.../source/<Type>Leveler.java` | novos arquivos |
| Registros bukkit | `BukkitAbilityManager`, `BukkitManaAbilityManager`, `BukkitLevelManager` | modificado (append) |
| Menu `/skills` | `bukkit/src/main/resources/menus/skills.yml` | modificado (append) |
| Comando `/<skill>` | `bukkit/.../commands/SkillCommands.java`, `CommandRegistrar.java` | modificado (append) |
| SourceTags (se necessário) | `common/.../source/SourceTag.java` | modificado (append, só se usado) |

**Convenções respeitadas:**
- Ordem das abilities em `skills.yml` é semântica: o unlock em `abilities.yml` usa `'{start}+N'` pela posição na lista.
- A 2ª ability de cada skill é a multiplicadora de XP e é a passada no construtor do enum `Skills`.
- O construtor de `Abilities` recebe o nome string da skill (`"trading"`) para migração de configs legadas.
- Skills habilitadas por padrão (como as 11 ativas); desabilitar em `skills.yml` oculta sources, abilities e menus automaticamente.
- Níveis são persistidos genericamente por nome de skill — nenhuma migração de banco necessária.
- Leaderboards, placeholders (`%auraskills_trading%` etc.), power/average, flags do WorldGuard e jobs funcionam sem código adicional (iteram skills habilitadas dinamicamente).

**Fórmula de XP por nível:** usa a mesma fórmula global de `xp_requirements.yml` (`multiplier * (level - 2)^2 + base`), sem overrides por skill — paridade com as skills existentes.

## 4. Skill: Trading (Comércio)

**Conceito:** progredir negociando — trocas com villagers e wandering traders, bartering com piglins. Skill de economia/utilidade.

### 4.1 XP sources (`sources/trading.yml`)

Novo source type `trading` (parser `TradingSourceParser`, leveler `TradingLeveler`).

- **`villager_trade`** (PlayerTradeEvent do Paper): XP por trade concluído. Valor configurável por item resultante, com multiplicadores configuráveis por profissão do villager e por tier (novice→master). Default: XP base 10 por trade de tier 1, +50% por tier adicional.
- **`wandering_trader_trade`**: mesma mecânica, XP base 15 (trades mais raros).
- **`piglin_barter`** (seção própria no mesmo arquivo, via PiglinBarterEvent): XP por item recebido no barter, valor por item (default 5; itens raros como netherite scrap configuráveis com valor maior).
- Cada source tem `menu_item` definido para o menu de sources.

**Anti-abuso:**
- XP decai ao repetir o mesmo trade no mesmo villager: janela deslizante de 5 trades; do 3º trade idêntico em diante, XP cai 50% por repetição até um piso de 10%.
- Trades de villagers curados (zombie discount) dão XP normal — o desconto de preço não afeta XP.
- Herda o anti-AFK e checagens de região do `SourceLeveler` base.
- Leveler em prioridade MONITOR: eventos cancelados por proteções de região não geram XP.

### 4.2 Stat rewards (`rewards/trading.yml`)

- 1 Luck por nível (intervalo 1)
- 1 Wisdom a cada 2 níveis (intervalo 2)

### 4.3 Abilities

| # | ID | Efeito | Valores iniciais (abilities.yml) |
|---|----|--------|----------------------------------|
| 1 | `silver_tongue` | Chance de reembolsar parte das esmeraldas gastas após cada trade | base 2% chance, +2%/nível, reembolso 10%, max_level 10 |
| 2 | `merchant` | Multiplicador de XP de Trading | segue o padrão das XP-multiplier abilities existentes |
| 3 | `charisma` | Chance de aplicar desconto nos preços ao abrir a interface de trade | base 3% chance, +3%/nível, desconto 5–15% por nível da ability |
| 4 | `master_negotiator` | Chance de um trade não consumir "uso" (não esgota o estoque do trade) | base 2%, +2%/nível, cap 50% |
| 5 | `guild_reputation` | Chance de receber o item do trade em dobro | base 1%, +1%/nível, cap 20% |

Unlocks: `'{start}+0'` escalonados pela posição, seguindo o espaçamento das skills existentes.

### 4.4 Mana ability: `grand_bargain`

Ativa (padrão `speed_mine`): por **10s base (+2s/nível)**, todos os trades têm **desconto adicional de 20%** e **100% de chance de não consumir uso**. Cooldown 300s, custo de mana 50 (valores iniciais em `mana_abilities.yml`). Implementado como `ManaAbilityProvider` (`GrandBargain.java`) que marca o jogador como ativo; `TradingAbilities` consulta o estado ao abrir/fechar trades.

**Cap de desconto:** o desconto total (charisma + grand_bargain + Hero of the Village) é limitado a 80% para não zerar preços.

### 4.5 Implementação

- `TradingAbilities.java`: listener de PlayerTradeEvent, InventoryOpenEvent (Merchant) e InventoryCloseEvent; manipula `MerchantRecipe` (ajuste de ingredientes para desconto, controle de `uses`).
- Preços modificados apenas na instância aberta do merchant — nenhuma alteração permanente no villager.
- Ícone do menu: `emerald`.

## 5. Skill: Husbandry (Criação)

**Conceito:** progredir criando e cuidando de animais — breeding, doma, shear e ordenha. Skill pacífica de fazenda.

### 5.1 XP sources (`sources/husbandry.yml`)

Novo source type `breeding` (parser `BreedingSourceParser`, leveler `BreedingLeveler`), com triggers no mesmo tipo:

- **`breed`** (EntityBreedEvent): XP por reproduzir, configurável por espécie. Defaults: animais comuns (vaca, ovelha, porco, galinha) 8 XP; cavalos/llamas 20 XP; axolotl/turtle/sniffer/camel 35 XP; mula (cruzamento) 40 XP.
- **`tame`** (EntityTameEvent): XP por domesticar — lobo 25, gato 25, papagaio 30, cavalo 20, llama 20.
- **`shear`** (PlayerShearEntityEvent): XP por tosquiar — ovelha 5, mooshroom 8, snow golem 5.
- **`milk`** (PlayerBucketFillEntityEvent, quando aplicável): ordenhar vaca/cabra/mooshroom — 3 XP.

**Anti-abuso:**
- XP de breed só conta quando o jogador é o breeder registrado pelo evento (alimentou os animais).
- Cooldown por casal de pais (UUIDs): o mesmo par não gera XP novamente por 5 minutos.
- Ovelhas recoloridas não geram XP de shear extra além do ciclo natural de crescimento da lã (checagem de `isSheared`/idade adulta).
- Prioridade MONITOR, ignora cancelados; anti-AFK do `SourceLeveler` herdado.

### 5.2 Stat rewards (`rewards/husbandry.yml`)

- 1 Health por nível
- 1 Regeneration a cada 2 níveis

### 5.3 Abilities

| # | ID | Efeito | Valores iniciais |
|---|----|--------|------------------|
| 1 | `twins` | Chance de nascer um 2º filhote em cada breeding | base 3%, +3%/nível, cap 30% |
| 2 | `rancher` | Multiplicador de XP de Husbandry | padrão das XP-multiplier abilities |
| 3 | `healthy_growth` | Filhotes gerados por você crescem mais rápido | 2%/nível de aceleração na maturação, cap 40% |
| 4 | `gentle_hands` | Chance de doma aumentada; animais domados por você ganham vida extra permanente | +5%/nível na chance de tame; +0.5 coração/nível, cap 5 corações |
| 5 | `bountiful_pasture` | Shear/ordenha rendem produtos extras | base 5% chance de item extra, +5%/nível |

### 5.4 Mana ability: `animal_whisperer`

Ativa: por **15s base (+3s/nível)**, num raio de **15 blocos**: animais adultos seguem o jogador sem isca, entram em love mode sem consumir alimento (respeitando o cooldown de breeding vanilla) e filhotes avançam instantaneamente uma etapa de crescimento (uma vez por ativação por filhote). Cooldown 300s, mana 50.

### 5.5 Implementação

- `HusbandryAbilities.java`: listeners de EntityBreedEvent, EntityTameEvent, PlayerShearEntityEvent, PlayerInteractEntityEvent.
- `twins`: spawna filhote adicional com mesmos pais; `healthy_growth`: tarefa periódica curta que aplica `setAge` acelerado em filhotes rastreados (rastreamento em memória, não persistido — filhotes não rastreados após restart simplesmente crescem normal).
- `gentle_hands`: vida extra via atributo `max_health` persistente no animal (NBT/attribute), aplicado uma única vez.
- `AnimalWhisperer.java`: ManaAbilityProvider com task de efeito em área enquanto ativo.
- Ícone do menu: `wheat`.

## 6. Skill: Smithing (Ferraria)

**Conceito:** progredir forjando, fundindo e craftando equipamentos — a skill do ferreiro.

**Decisão de design:** bigorna e grindstone **continuam dando XP de Enchanting** (como hoje). Smithing cobre smithing table, fundição e crafting. Não há fonte de XP duplicada entre skills.

### 6.1 XP sources (`sources/smithing.yml`)

Três novos source types:

- **`smithing`** (SmithItemEvent; parser `SmithingSourceParser`, leveler `SmithingLeveler`): XP por uso da smithing table. Defaults: upgrade para netherite 50 XP; armor trim 15 XP; por tipo de resultado configurável.
- **`smelting`** (FurnaceExtractEvent; `SmeltingSourceParser`, `SmeltingLeveler`): XP por item retirado manualmente de fornalha/forno/defumador. Defaults: minérios fundidos 4 XP/item; comida 2 XP/item; vidro/pedra 1 XP/item.
- **`crafting`** (CraftItemEvent; `CraftingSourceParser`, `CraftingLeveler`): XP por craftar, configurável por item/grupo. Defaults: ferramentas/armaduras de diamante 25 XP, ferro 10 XP, netherite via smithing (não crafting); armas idem; itens triviais (sticks, torches, botões, placas) 0 XP via lista de exclusão.

**Anti-abuso:**
- Smelting conta apenas a quantidade retirada pelo jogador no slot de resultado; extração por hopper não dispara o evento (não gera XP).
- Crafting: XP proporcional à quantidade craftada (shift-click conta o stack inteiro); lista de exclusão para receitas de farm (deconstruct de blocos de minério, escadas↔blocos).
- Prioridade MONITOR, ignora cancelados; anti-AFK herdado.

### 6.2 Stat rewards (`rewards/smithing.yml`)

- 1 Toughness por nível
- 1 Strength a cada 2 níveis

### 6.3 Abilities

| # | ID | Efeito | Valores iniciais |
|---|----|--------|------------------|
| 1 | `efficient_smelting` | Chance de fundição render o dobro ao retirar da fornalha | base 2%, +2%/nível, cap 30% |
| 2 | `smith` | Multiplicador de XP de Smithing | padrão das XP-multiplier abilities |
| 3 | `master_crafted` | Ferramentas/armaduras craftadas têm chance de vir com Unbreaking já aplicado | base 5% chance, +5%/nível; nível do encantamento I→III conforme a ability sobe |
| 4 | `recycler` | Ao usar smithing table, chance de recuperar parte dos materiais consumidos | base 3%, +3%/nível, cap 25% |
| 5 | `forge_mastery` | Reduz custo de níveis de XP em bigorna e smithing table | 2%/nível, cap 40% |

### 6.4 Mana ability: `forge_overdrive`

Ativa: por **15s base (+3s/nível)**, fornalhas/fornos/defumadores num raio de **10 blocos** fundem **2x mais rápido** sem consumir combustível extra, e as retiradas manuais do jogador rendem o dobro garantido durante o efeito. Cooldown 300s, mana 50.

### 6.5 Implementação

- `SmithingAbilities.java`: listeners de SmithItemEvent, CraftItemEvent, PrepareAnvilEvent (redução de custo), FurnaceExtractEvent (bônus de retirada).
- `ForgeOverdrive.java`: ManaAbilityProvider; aceleração via FurnaceSmeltEvent (ajuste de cook time) em fornalhas no raio, reavaliadas por task periódica enquanto ativo.
- `recycler` usa um novo `SourceTag` (`SMITHING_APPLICABLE`) para filtrar sources elegíveis, seguindo o padrão de `TREECAPITATOR_APPLICABLE`.
- Ícone do menu: `netherite_upgrade_smithing_template` (evita confusão visual com o `anvil` associado ao tema de Enchanting).

## 7. Novos source types (detalhe técnico)

Cada novo tipo segue o molde dos 13 existentes:

1. **API:** interface em `api/.../api/source/type/` (ex.: `TradingSource.java`) estendendo `XpSource`, com getters específicos (ex.: item/profissão/tier).
2. **Registro:** constante em `SourceTypes.java` + registro em `SourceTypeRegistry` (bloco append-only marcado).
3. **Parser:** `common/.../source/parser/<Type>SourceParser.java`, desserializando entradas de `sources/<skill>.yml` com suporte a placeholders `{key}`/`{value}` e `menu_item`/`unit`, como os parsers existentes.
4. **Leveler:** `bukkit/.../source/<Type>Leveler.java` estendendo `SourceLeveler` (herda checagem de skill desabilitada, anti-AFK, regiões), registrado em `BukkitLevelManager.registerLevelers()` (bloco append-only).
5. O mapeamento source→skill continua por arquivo (sources no `sources/trading.yml` dão XP de Trading), sem mudança no `SkillManager`.

## 8. Casos de borda e tratamento de erros

- **Skill desabilitada:** comportamento existente cobre — sources/abilities/menus se ocultam.
- **Eventos cancelados por outros plugins:** todos os levelers em MONITOR ignoram cancelados.
- **Proteções de região (WorldGuard):** flags existentes iteram `Skills.values()` — as 3 novas skills são cobertas automaticamente.
- **Servidores com configs antigas:** `SourceFileUpdates`/`MenuFileUpdates` (entradas append-only) entregam os novos arquivos via bump de `file_version`.
- **Restart do servidor:** estado volátil (filhotes rastreados, fornalhas em overdrive, janelas de decaimento de trade) não é persistido; na pior hipótese o efeito simplesmente não se aplica — sem corrupção de dados.
- **Sem novas dependências externas:** reembolso de esmeraldas é em item; nenhum hook novo (Vault etc.).
- **Minecraft 1.20–26.x:** todos os eventos usados (PlayerTradeEvent, PiglinBarterEvent, EntityBreedEvent, EntityTameEvent, PlayerShearEntityEvent, SmithItemEvent, FurnaceExtractEvent, CraftItemEvent) existem em todo o range suportado. Onde a API Paper divergir entre versões, usar o mesmo padrão de compatibilidade já presente no código (verificação de classe/método ou abstração no módulo `paper`).

## 9. Testes

- **Parsers:** testes unitários para cada novo source parser, seguindo os testes de parser existentes em `common` (desserialização, defaults, placeholders).
- **Carregamento:** teste de `SkillLoader`/`SourceLoader` com as 3 skills presentes em `skills.yml` + sources + rewards (arquivos de fixture nos testFixtures).
- **Fórmulas:** testes unitários das funções puras de chance/decaimento (decaimento de trade, caps de desconto, chance de twins) onde isoláveis de Bukkit.
- **Verificação manual em servidor Paper:** XP concedido em cada ação; abilities disparando com chance; mana abilities com duração/cooldown; menu `/skills` mostrando 14 ícones alinhados (group/order); leaderboard e placeholders atualizando; `/trading`, `/husbandry`, `/smithing` abrindo a skill correta.

## 10. Ordem sugerida de implementação

1. Infraestrutura: novos source types (API + parsers + levelers) com testes de parser
2. Smithing (usa 3 source types novos — valida a infraestrutura cedo)
3. Husbandry (1 source type com múltiplos triggers)
4. Trading (1 source type + lógica de merchant mais delicada)
5. Configs/mensagens/menus/comandos das 3 (append-only, blocos marcados)
6. Balanceamento inicial em servidor de teste
