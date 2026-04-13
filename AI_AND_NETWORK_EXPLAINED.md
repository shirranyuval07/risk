# Risk: Global Conquest — Technical Deep Dive

## Part 1: The AI Algorithm

---

### 1.1 High-Level Architecture

The AI system follows a **Strategy + Graph Analysis** architecture. It is built from five layers:

```
application.yml          ← weights & thresholds (data)
       ↓
AIProperties / AIStrategyProps   ← Spring reads YAML into POJOs
       ↓
AIConfig                 ← builds 3 HeuristicStrategy.Configurable beans
       ↓
AIEngine.Factory         ← creates GreedyAI(strategy) instances
       ↓
GreedyAI + AIGraphAnalyzer      ← executes the actual game turn
```

Every AI player is a **`GreedyAI`** instance. What makes each bot behave differently (balanced / defensive / offensive) is the **`HeuristicStrategy`** object injected into it. The strategy holds:
- **Weighted scoring rules** (for attacks & setup placement)
- **Behavioral lambdas** (for draft placement & troop movement after conquest)
- **Threshold values** (when to attack, minimum army advantage, etc.)

This means **zero subclasses are needed** — one `GreedyAI` class drives all three personalities purely through configuration.

---

### 1.2 Configuration Pipeline (YAML → Spring Beans → GreedyAI)

#### Step 1: `application.yml`

All AI personality differences live in this file under the `ai:` prefix. There are three sections — `balanced`, `defensive`, `offensive` — each with ~15 weight/threshold fields:

```yaml
ai:
  balanced:
    weight-win-probability: 1.5      # how much the bot values a high chance of winning the fight
    weight-continent-bonus: 1.0      # how much continent progress matters in attack scoring
    weight-strategic-value: 1.5      # weight of isolation + articulation bonuses
    weight-expected-casualties: 2.5  # penalty for expected losses
    articulation-point-bonus: 5.0    # bonus for attacking enemy articulation points
    casualties-multiplier: 1.2       # scales raw casualty estimate
    exposure-penalty-multiplier: 2.0 # multiplies cost when source becomes exposed
    attack-threshold: 0.2            # minimum heuristic score to consider an attack
    min-army-advantage: 2            # must have ≥2 more armies than defender
    weight-future-threat: 0.5        # weight of futureThreatRule
    bonus-focus: 0.8                 # continent bonus sub-weight
    progress-focus: 0.5              # continent progress sub-weight
    continent-break-multiplier: 0.5  # reward for breaking an enemy's continent
    resistance-avoidance: 1.0        # penalty for attacking well-defended continents
    setup-stacking-weight: 3.0       # how much the bot prefers stacking in setup
```

**Key personality differences:**
| Weight | Balanced | Defensive | Offensive |
|--------|----------|-----------|-----------|
| `attack-threshold` | 0.2 | 0.2 | **-1.0** (attacks almost anything) |
| `min-army-advantage` | 2 | 2 | **1** (more reckless) |
| `weight-expected-casualties` | 2.5 | 2.0 | **0.2** (doesn't care about losses) |
| `articulation-point-bonus` | 5.0 | **15.0** (highly values critical points) | 1.0 |
| `setup-stacking-weight` | 3.0 | 2.0 (spreads across borders) | **20.0** (concentrates for attack) |
| `continent-break-multiplier` | 0.5 | 0.0 | **3.0** (loves breaking enemy continents) |
| `weight-future-threat` | 0.5 | 0.0 | **-0.2** (ignores threats) |

#### Step 2: `AIProperties` + `AIStrategyProps`

Spring Boot auto-binds the YAML to Java objects:

```java
// AIProperties.java — reads "ai:" prefix
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AIProperties {
    @NestedConfigurationProperty
    private AIStrategyProps balanced;   // ← ai.balanced.*
    @NestedConfigurationProperty
    private AIStrategyProps defensive;  // ← ai.defensive.*
    @NestedConfigurationProperty
    private AIStrategyProps offensive;  // ← ai.offensive.*
}
```

```java
// AIStrategyProps.java — one POJO per personality
@Data
public class AIStrategyProps {
    private double weightWinProbability;
    private double weightContinentBonus;
    // ... all 15 fields ...
    private double setupStackingWeight;
}
```

#### Step 3: `AIConfig` — Building Strategy Beans

`AIConfig` is a `@Configuration` class that creates three `HeuristicStrategy.Configurable` beans:

```java
@Bean
public HeuristicStrategy.Configurable balancedStrategy() {
    return buildStrategy(props.getBalanced(),
            (totalArmies, minMove, maxMove) -> Math.max(minMove, Math.min(maxMove, totalArmies - 3)),
            HeuristicStrategy.Configurable.DraftBehavior.defensive());
}

@Bean
public HeuristicStrategy.Configurable offensiveStrategy() {
    return buildStrategy(props.getOffensive(),
            (totalArmies, minMove, maxMove) -> maxMove,  // move ALL troops forward
            HeuristicStrategy.Configurable.DraftBehavior.aggressive());
}
```

Each bean gets:
1. **`HeuristicWeights`** record — all scoring weights packed into one object
2. **`ThresholdConfig`** record — `attackThreshold`, `minArmyAdvantage`, `setupStackingWeight`
3. **`TroopMovementBehavior`** lambda — how many troops to move after conquering
4. **`DraftBehavior`** lambda — how to place reinforcements

The `buildStrategy()` helper also wires in attack heuristic rules and setup heuristic rules inside the `Configurable` constructor.

#### Step 4: `AIEngine.Factory` — Creating GreedyAI Instances

```java
@Component
public static class Factory {
    public BotStrategy createAI(HeuristicStrategy strategy) {
        return new GreedyAI(strategy);
    }
}
```

#### Step 5: `RiskApplication.startGameWithConfig()` — Wiring to Players

```java
HeuristicStrategy balancedStrategy = springContext.getBean("balancedStrategy", HeuristicStrategy.class);
AIEngine.Factory aiFactory = springContext.getBean(AIEngine.Factory.class);

// For each AI player setup:
case "AI - Balanced"  -> p.setStrategy(aiFactory.createAI(balancedStrategy));
case "AI - Defensive" -> p.setStrategy(aiFactory.createAI(defensiveStrategy));
case "AI - Offensive" -> p.setStrategy(aiFactory.createAI(offensiveStrategy));
```

---

### 1.3 Turn Execution Flow

When it's an AI player's turn, `AIEngine.Service.playTurn()` is called:

```
AIEngine.Service.playTurn(aiPlayer, game)
  │
  ├─ Is it Setup phase?
  │    └─ YES → aiPlayer.getStrategy().findSetUpCountry() → game.placeArmy(country)
  │
  ├─ Trade cards (if any valid sets)
  │    └─ cardService.tradeAnyValidSet(aiPlayer) → adds bonus draft armies
  │
  └─ aiPlayer.getStrategy().executeTurn(aiPlayer, game)
       │
       └─ GreedyAI.executeTurn():
            ├─ 1. chooseReinforcement()  → strategy.executeDraft()
            ├─ game.nextPhase()
            ├─ 2. chooseAttack()         → build priority queue → attack loop
            ├─ game.nextPhase()
            ├─ 3. chooseFortify()        → move trapped/border armies
            └─ game.nextPhase()
```

---

### 1.4 Setup Phase — Rule-Based Country Scoring

**File:** `SetupHeuristicRule.java` (functional interface) + `HeuristicStrategy.Abstract.calculateSetupScore()`

During setup, the AI must choose which of its owned countries to place one army on. It scores **every owned country** using 4 pluggable rules, each multiplied by a weight:

```
score(country) = Σ  rule.evaluate(country) × weight
```

The 4 rules wired in `Configurable`'s constructor:

```java
this.addSetupRule(SetupHeuristicRule.enemyThreatRule(),        1.0);
this.addSetupRule(SetupHeuristicRule.stackingRule(),           thresholds.setupStackingWeight());
this.addSetupRule(SetupHeuristicRule.continentProgressRule(),  weights.continentBonus());
this.addSetupRule(SetupHeuristicRule.borderCoverageRule(),     weights.strategicValue());
```

#### Rule 1: `enemyThreatRule()` (weight = 1.0 for all)

```java
return (country, player, analyzer) -> {
    int totalEnemyStrength = 0;
    for (Country neighbor : country.getNeighbors())
        if (neighbor.getOwner() != player)
            totalEnemyStrength += neighbor.getArmies();
    return totalEnemyStrength;
};
```
**What it does:** Sums enemy armies around the country. Countries surrounded by strong enemies get higher scores → the bot reinforces them first.

#### Rule 2: `stackingRule()` (weight varies: balanced=3, defensive=2, offensive=20)

```java
return (country, player, analyzer) -> Math.log(country.getArmies() + 1);
```
**What it does:** Encourages placing more armies where you already have armies (stacking). Uses `log()` for **diminishing returns** — without it, the rule creates a snowball effect where the bot dumps everything on one country.

**Why `log()`?** If country A has 10 armies and country B has 1 army:
- Linear: A scores 10, B scores 1 → 10× preference → snowball
- Log: A scores 2.4, B scores 0.69 → 3.5× preference → moderate stacking

The offensive bot (weight=20) stacks heavily because `20 × log(11) = 48` — it concentrates forces at one point to create a devastating attack force. The defensive bot (weight=2) barely stacks, so other rules like `borderCoverageRule` (weight=2) and `enemyThreatRule` (weight=1) dominate, spreading armies across threatened borders for defense. Balanced (weight=3) sits in the middle.

#### Rule 3: `continentProgressRule()` (weight = `continentBonus`: balanced=1, defensive=0.5, offensive=3)

```java
return (country, player, analyzer) -> {
    double progressRatio = (double) ownedCountries / totalCountries;
    return progressRatio * continent.getBonusValue();
};
```
**What it does:** Rewards placing in continents you're close to completing. Offensive (weight=3) aggressively pushes toward continent bonuses.

#### Rule 4: `borderCoverageRule()` (weight = `strategicValue`: balanced=1.5, defensive=2, offensive=0.5)

```java
return (country, player, analyzer) -> analyzer.countEnemyNeighbors(country, player);
```
**What it does:** Countries with many enemy neighbors get higher scores — they are border countries that need reinforcement.

#### How scores are computed:

```java
// HeuristicStrategy.Abstract.calculateSetupScore()
public double calculateSetupScore(Country country, Player player, AIGraphAnalyzer analyzer) {
    double score = 0;
    for (var rule : setupRules.entrySet())
        score += rule.getKey().evaluate(country, player, analyzer) * rule.getValue();
    return score;
}
```

The country with the highest score is chosen by `AIGraphAnalyzer.findBestSetupCountry()`:

```java
public Country findBestSetupCountry(Player player, HeuristicStrategy strategy) {
    Country bestCountry = null;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (Country country : player.getOwnedCountries()) {
        double score = strategy.calculateSetupScore(country, player, this);
        if (score > bestScore) {
            bestCountry = country;
            bestScore = score;
        }
    }
    return bestCountry != null ? bestCountry : player.getOwnedCountries().getFirst();
}
```

---

### 1.5 Draft Phase — Behavioral Lambdas

**File:** `HeuristicStrategy.Configurable.DraftBehavior` (functional interface)

Unlike setup (which uses weighted scoring to pick **where**), draft determines **how** to distribute reinforcement armies. This requires entirely different algorithms for aggressive vs defensive play, so it uses a **behavioral lambda** instead of weighted rules.

```java
@FunctionalInterface
public interface DraftBehavior {
    void execute(Player player, RiskGame game, AIGraphAnalyzer analyzer, HeuristicStrategy strategy);
}
```

#### Aggressive Draft (used by offensive):

```java
static DraftBehavior aggressive() {
    return (player, game, analyzer, strategy) -> {
        AttackMove best = analyzer.findBestPotentialAttack(player, strategy);
        if (best != null)
            while (player.getDraftArmies() > 0) game.placeArmy(best.source());
    };
}
```
**Algorithm:** Find the single best attack opportunity, then dump **all** reinforcement armies onto that source country. This creates a massive force at one point for a devastating attack.

#### Defensive Draft (used by balanced & defensive):

```java
static DraftBehavior defensive() {
    return (player, game, analyzer, strategy) -> {
        Set<Country> bottlenecks = analyzer.findArticulationPoints(player);
        Map<Country, Double> threatScores = analyzer.calculateThreatScores(player, bottlenecks);
        double totalThreat = threatScores.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalDraftArmies = player.getDraftArmies();

        // Proportional distribution: each country gets armies proportional to its threat score
        for (Map.Entry<Country, Double> entry : threatScores.entrySet()) {
            int armies = (int) Math.floor((entry.getValue() / totalThreat) * totalDraftArmies);
            for (int i = 0; i < armies; i++)
                if (player.getDraftArmies() > 0) game.placeArmy(entry.getKey());
        }

        // Remainder goes to the most threatened country
        Country mostThreatened = analyzer.findMostThreatenedCountry(player);
        while (player.getDraftArmies() > 0 && mostThreatened != null)
            game.placeArmy(mostThreatened);
    };
}
```
**Algorithm:**
1. Find articulation points (critical structural nodes)
2. Calculate threat scores for each border country (enemy strength / own strength, ×2 if articulation point)
3. Distribute armies proportionally to threat scores
4. Leftover armies go to the most threatened country

**Why keep Draft separate from Setup?** Setup uses **scoring** (all rules contribute to one number per country — right tool for "pick the best single country"). Draft uses **algorithm-level branching** (aggressive dumps everything on one point vs defensive distributes proportionally — you can't express "dump everything on one point" as a weighted sum of rules).

---

### 1.6 Attack Phase — Heuristic Scoring + Priority Queue

**Files:** `GreedyAI.chooseAttack()`, `AIGraphAnalyzer.buildAttackQueue()`, `HeuristicStrategy.Abstract.calculateHeuristic()`

#### Building the Attack Queue

```java
// AIGraphAnalyzer.buildAttackQueue()
for (Country source : player.getOwnedCountries()) {
    if (source.getArmies() > 1) {
        for (Country target : source.getNeighbors()) {
            if (target.getOwner() != player &&
                source.getArmies() - target.getArmies() >= strategy.getMinArmyAdvantage()) {
                
                double score = strategy.calculateHeuristic(source, target, player, this);
                
                if (score > strategy.getAttackThreshold())
                    queue.add(new AttackMove(source, target, score));
            }
        }
    }
}
```

For **every** owned country with >1 army, check **every** enemy neighbor. If the army advantage meets the threshold, compute the heuristic score. If the score exceeds `attackThreshold`, add to a max-priority queue.

**Offensive** has `attackThreshold = -1.0` and `minArmyAdvantage = 1`, so it considers almost every possible attack. **Balanced/Defensive** have `attackThreshold = 0.2` and `minArmyAdvantage = 2`, filtering out risky attacks.

#### The Heuristic Formula

```
score = baseScore + dynamicScore + easyWinBonus
```

**Base Score** = `(winProbability × weight) + (strategicValue × weight) - (expectedCost × weight)`

- **winProbability**: from `WinProbabilityCalculator.estimate()` (see §1.7)
- **strategicValue**: isolation bonus + articulation point bonus
  - Isolation bonus: fewer enemy neighbors → safer → higher value
  - Articulation bonus: if target is an articulation point of the enemy's graph → breaking it splits their network
- **expectedCost**: estimated losses × exposure penalty if source becomes vulnerable after attack

**Dynamic Score** = sum of attack heuristic rules:

```java
for (var rule : dynamicRules.entrySet())
    score += rule.getKey().evaluate(source, target, player, analyzer) * rule.getValue();
```

Two attack rules are wired for all personalities:

1. **`futureThreatRule()`** — looks at the target's neighbors. If there's a strong enemy nearby, the score is penalized (you'll be counter-attacked). Offensive gets weight `-0.2` (ignores threats), balanced gets `0.5`.

2. **`continentProgressRule()`** — rewards attacking territories that bring you closer to completing a continent. Considers: progress ratio, continent bonus value, enemy resistance in continent, whether you'd break an enemy's completed continent.

One extra rule for **defensive only**:

3. **`cardFarmingRule()`** — massive bonus for attacking weak targets (≤2 armies) when you're strong (≥4 armies). This encourages defensive bots to do "safe" attacks just to earn cards.

**Easy Win Bonus** — special case for endgame situations:
- If the enemy has only **1 territory left**: huge bonus to finish them off (and collect their cards)
- If the enemy has **1-2 territories**, is **surrounded** by us, and is **weak**: strong bonus

#### Attack Execution Loop

```java
private void chooseAttack(Player player, RiskGame game) {
    MaxPriorityQueue<AttackMove> attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);

    while (!attackQueue.isEmpty()) {
        AttackMove best = attackQueue.poll();

        if (isMoveStillValid(best, player)) {
            boolean conquered = false;
            while (isMoveStillValid(best, player) && !conquered)
                conquered = performAttack(best, game);

            if (conquered)
                attackQueue = graphAnalyzer.buildAttackQueue(player, strategy);
                // ↑ Rebuild queue: conquering may open new opportunities
        }
    }
}
```

The AI:
1. Polls the highest-scored attack
2. Validates it's still possible (owner hasn't changed, still enough armies)
3. Attacks repeatedly until conquest or invalid
4. After each conquest, **rebuilds the entire queue** to discover new attack opportunities created by the conquest
5. Handles troop movement after conquest via `strategy.getTroopsToMoveAfterConquest()`

#### Post-Conquest Troop Movement (`TroopMovementBehavior`):
- **Balanced:** `Math.max(minMove, Math.min(maxMove, totalArmies - 3))` — keep 3 behind
- **Defensive:** `Math.max(minMove, Math.min(maxMove, totalArmies / 2))` — split 50/50
- **Offensive:** `maxMove` — move everything forward, full aggression

---

### 1.7 Win Probability Calculator

**File:** `WinProbabilityCalculator.java`

Based on research by Osborne (2003) and Georgiou (2004), this class estimates the probability of winning a Risk battle.

Three tiers:

| Army Size | Method | How |
|-----------|--------|-----|
| ≤10 vs ≤10 | **Markov chain lookup table** | Pre-computed 11×11 matrix of exact probabilities |
| 11-50 | **Mathematical approximation** | Ratio-based with Osborne's equilibrium point (0.924) |
| >50 | **Law of large numbers** | Linear interpolation around 0.85 transition zone |

```java
public double estimate(int attackerArmies, int defenderArmies) {
    int actualAttackers = attackerArmies - 1; // one must stay behind

    if (canUseLookupTable(actualAttackers, defenderArmies))
        return MARKOV_PROBABILITIES[actualAttackers][defenderArmies];

    double ratio = (double) actualAttackers / defenderArmies;

    if (isMassiveBattle(actualAttackers, defenderArmies))
        return estimateMassiveBattleProbability(ratio);

    return estimateMediumBattleProbability(ratio);
}
```

Example lookup values: 3 attackers vs 3 defenders → 20% win chance. 6 vs 3 → 52%. 10 vs 5 → 52%.

---

### 1.8 Fortify Phase — Two-Stage Strategy

**File:** `AIGraphAnalyzer.calculateBestFortify()`

```
Stage 1: Find "trapped" armies → move to nearest border
Stage 2: Move from safest border → most threatened border
```

#### Stage 1: Trapped Country Move

A **trapped country** is one where **all neighbors are owned by us**. Its armies are useless sitting there.

```java
for (Country source : player.getOwnedCountries()) {
    if (source.getArmies() > 1) {
        if (isCountryTrapped(source, player)) {
            Country border = findConnectedBorderUsingBFS(source, player);
            // Keep the trapped country with the most armies (highest priority)
        }
    }
}
```

The BFS (`bfsReachableOwned`) finds a connected border country (one with enemy neighbors) reachable through owned territory. All armies minus 1 are moved there.

#### Stage 2: Border Redistribution

If no trapped countries exist, the AI finds the **safest border** (lowest threat level) and the **most threatened border** (highest threat level), checks if they're connected via BFS, and moves armies from safe → threatened.

```java
double threatLevel = (double) totalEnemyForce / Math.max(border.getArmies(), 1);
```

---

### 1.9 Graph Algorithms

**File:** `AIGraphAnalyzer.java`

#### BFS — `bfsReachableOwned(start, player)`

Standard breadth-first search that traverses only countries owned by `player`. Used for:
- Finding connected border countries for fortify
- Checking if two countries are connected (`isConnectedBFS`)

#### DFS — Articulation Points (`findArticulationPoints`)

Uses Tarjan's algorithm (discovery time + low values) to find **articulation points** — countries that, if removed, would split the player's territory graph into disconnected components.

```
If removing country X disconnects your territory network → X is an articulation point
```

These are used:
- **Attack scoring:** Attacking an enemy's articulation point gets a bonus (breaks their network)
- **Draft defense:** Articulation points get 2× threat score → more reinforcements
- **Draft bottleneck detection:** The defensive draft specifically identifies these for prioritized reinforcement

The DFS runs in O(V+E) where V = owned countries and E = connections between them.

---

### 1.10 Class Diagram Summary

```
BotStrategy (interface)
  └── GreedyAI (implementation)
        ├── has-a → HeuristicStrategy (interface)
        │             └── Abstract (base class)
        │                   └── Configurable (concrete, holds lambdas + rules)
        │                         ├── has-a → DraftBehavior (λ)
        │                         ├── has-a → TroopMovementBehavior (λ)
        │                         ├── has-a → Map<HeuristicRule, weight> (attack rules)
        │                         └── has-a → Map<SetupHeuristicRule, weight> (setup rules)
        │
        └── has-a → AIGraphAnalyzer (graph algorithms)

AIEngine.Service → orchestrates AI turns (setup detection, card trading)
AIEngine.Factory → creates GreedyAI instances
AIConfig → builds Configurable beans from AIProperties (YAML)

Records: AttackMove, BattleResult, FortifyMove
```

---

## Part 2: The Network Package

---

### 2.1 Architecture Overview

The multiplayer system uses **WebSocket** for real-time bidirectional communication. The architecture is:

```
  Player A (Host)                    Player B (Client)
  ┌─────────────┐                   ┌─────────────┐
  │ JavaFX App  │                   │ JavaFX App  │
  │ (--server)  │                   │ (client)    │
  │             │                   │             │
  │ ┌─────────┐ │                   │ ┌─────────┐ │
  │ │WebSocket│ │                   │ │WebSocket│ │
  │ │ Client  │─│───┐         ┌────│─│ Client  │ │
  │ └─────────┘ │   │         │    │ └─────────┘ │
  │             │   │         │    │             │
  │ ┌─────────┐ │   │         │    └─────────────┘
  │ │ Spring  │ │   │         │
  │ │ Boot    │ │   ▼         │
  │ │ Server  │◄├─── ngrok ◄──┘
  │ └─────────┘ │  (tunnel)
  └─────────────┘
```

**Key insight:** The host runs both the Spring Boot server AND a JavaFX client. The Spring Boot server handles WebSocket connections. **ngrok** creates a public tunnel so players on different networks can connect.

---

### 2.2 Server-Side Components

#### `WebSocketConfig.java` — Registration

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameWebSocketHandler, "/risk-ws").setAllowedOrigins("*");
    }
}
```

Registers the `GameWebSocketHandler` at the `/risk-ws` endpoint. `setAllowedOrigins("*")` allows connections from any origin (needed for ngrok tunneling).

#### `RoomManager.java` — Room Lifecycle

A Spring `@Component` that manages game rooms:

```java
private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();
private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
```

**`createRoom()`** — Generates a 4-character UUID-based room code (e.g., "A7B2"):
```java
String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
rooms.put(roomId, new ArrayList<>());
```

**`joinRoom(roomId, session)`** — Adds a WebSocket session to the room's player list. Returns `false` if the room doesn't exist.

**`broadcastToRoom(roomId, message)`** — Sends a text message to **all** sessions in the room:
```java
for (WebSocketSession session : sessions)
    if (session.isOpen())
        session.sendMessage(new TextMessage(message));
```

**`handleDisconnect(session)`** — Removes the session, notifies remaining players with a `PLAYER_DISCONNECTED` message, and cleans up empty rooms.

#### `GameWebSocketHandler.java` — Message Router

Extends Spring's `TextWebSocketHandler`. Each incoming message is deserialized into a `GameMessage` and routed by `type`:

| Action | Server Behavior |
|--------|----------------|
| `CREATE_ROOM` | Creates room via `RoomManager`, auto-joins creator, responds with `ROOM_CREATED` + room ID |
| `JOIN_ROOM` | Joins room, responds with `JOIN_ROOM_SUCCESS` + player index, broadcasts `PLAYER_JOINED` to room |
| `START_GAME` | Broadcasts `GAME_STARTED` to all players in the room (with game seed in content) |
| `ATTACK_REQ` | **Server rolls dice** (calls `Dice.rollBattle()`), broadcasts `BATTLE_RESULT` to all |
| `SETUP_PLACE`, `DRAFT`, `FORTIFY`, `CONQUEST_MOVE`, `NEXT_PHASE`, `NEXT_TURN` | **Pass-through:** broadcasts the raw message to all players in the room |

**Why does the server roll dice?** To ensure fairness — if dice were rolled client-side, a player could cheat by modifying their code.

```java
case ATTACK_REQ -> {
    int attackerArmies = (int) data.get("ATTACKER_ARMIES");
    int defenderArmies = (int) data.get("DEFENDER_ARMIES");
    BattleResult result = rollBattle(attackerArmies, defenderArmies);

    Map<String, Object> resultContent = new HashMap<>();
    resultContent.put("attackerId", data.get("ATTACK_REQ"));
    resultContent.put("defenderId", data.get("DESTINATION_ID"));
    resultContent.put("battleResult", result);

    roomManager.broadcastToRoom(gameMsg.roomId(), ...);
}
```

---

### 2.3 Shared Protocol

#### `GameMessage.java` — Message Format

```java
public record GameMessage(
    GameAction type,           // enum: what kind of action
    String roomId,             // 4-char room code
    String sender,             // player name or "Server"
    Map<String, Object> content // arbitrary payload
) {}
```

Serialized to/from JSON automatically by Jackson `ObjectMapper`. Example JSON:
```json
{
  "type": "DRAFT",
  "roomId": "A7B2",
  "sender": "Alice",
  "content": { "countryId": 15 }
}
```

#### `GameAction.java` — All Message Types

```java
public enum GameAction {
    // Room Management
    CREATE_ROOM, ROOM_CREATED, JOIN_ROOM, JOIN_ROOM_SUCCESS, PLAYER_JOINED, ERROR,

    // Game State
    START_GAME, GAME_STARTED, GAME_ACTION, NEXT_PHASE, NEXT_TURN,

    // In-Game Actions
    SETUP_PLACE, DRAFT, ATTACK_REQ, BATTLE_RESULT, CONQUEST_MOVE, FORTIFY
}
```

---

### 2.4 Client-Side

#### `RiskWebSocketClient.java`

Uses Java's built-in `java.net.http.HttpClient` WebSocket API (no external libraries).

**Connection:**
```java
private static final String SERVER_URI = "wss://....ngrok-free.dev/risk-ws";

public void connect() {
    HttpClient client = HttpClient.newHttpClient();
    client.newWebSocketBuilder()
        .header("ngrok-skip-browser-warning", "true")  // required for ngrok
        .buildAsync(URI.create(SERVER_URI), this);      // 'this' = WebSocket.Listener
}
```

The `ngrok-skip-browser-warning` header is required because ngrok normally shows a warning page for free-tier tunnels.

**Sending messages:**
```java
public void sendAction(GameAction type, String roomId, Map<String,Object> content) {
    GameMessage msg = new GameMessage(type, roomId, this.playerName, content);
    String jsonMessage = objectMapper.writeValueAsString(msg);
    webSocket.sendText(jsonMessage, true);
}
```

**Receiving messages:**
```java
@Override
public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    GameMessage msg = objectMapper.readValue(data.toString(), GameMessage.class);

    // Route to JavaFX UI thread
    if (onMessageReceived != null)
        Platform.runLater(() -> onMessageReceived.accept(msg));

    webSocket.request(1);  // request next message (back-pressure control)
    return CompletableFuture.completedFuture(null);
}
```

**Critical detail:** `Platform.runLater()` ensures the message handler runs on the JavaFX Application Thread, since WebSocket callbacks arrive on a network thread and UI updates must happen on the FX thread.

**Connection readiness:** Uses `CompletableFuture<WebSocket> connectionReady` so the UI can `waitForConnection()` before sending messages.

**Disconnection:**
```java
public void disconnect() {
    if (this.webSocket != null)
        this.webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Returning to main menu");
}
```

---

### 2.5 Complete Multiplayer Flow

#### Creating a Lobby (Host)

```
1. Host launches app with --server flag
   └─ Spring Boot starts, registers /risk-ws endpoint
   └─ ngrok tunnel is running externally, forwarding to localhost

2. Host clicks "Create Game" in MainMenu
   └─ new RiskWebSocketClient(playerName).connect()
   └─ Waits for connectionReady future
   └─ sendAction(CREATE_ROOM, null, {})

3. Server receives CREATE_ROOM
   └─ RoomManager.createRoom() → roomId = "A7B2"
   └─ RoomManager.joinRoom("A7B2", hostSession)
   └─ Sends back: { type: ROOM_CREATED, roomId: "A7B2" }

4. Host receives ROOM_CREATED
   └─ client.setRoomId("A7B2")
   └─ UI shows LobbyScreen with room code "A7B2"
```

#### Joining a Lobby (Client)

```
1. Client launches app (no --server flag)
   └─ No Spring Boot, no server

2. Client enters room code "A7B2", clicks Join
   └─ new RiskWebSocketClient(playerName).connect()
   └─ Connects via WSS to the ngrok URL
   └─ sendAction(JOIN_ROOM, "A7B2", {})

3. Server receives JOIN_ROOM
   └─ RoomManager.joinRoom("A7B2", clientSession) → true
   └─ Sends to client: { type: JOIN_ROOM_SUCCESS, playerID: 2 }
   └─ Broadcasts to room: { type: PLAYER_JOINED, PlayerNameWithID: "Bob2" }

4. Client receives JOIN_ROOM_SUCCESS
   └─ UI shows LobbyScreen
   
5. Host receives PLAYER_JOINED
   └─ UI updates player list
```

#### Starting the Game

```
1. Host clicks "Start Game"
   └─ Generates a random seed
   └─ sendAction(START_GAME, "A7B2", { seed: 12345 })

2. Server receives START_GAME
   └─ Broadcasts { type: GAME_STARTED, seed: 12345 } to all players

3. All clients receive GAME_STARTED
   └─ client.setGameSeed(12345)
   └─ startGameWithConfig() is called
   └─ RiskGame is created with the shared seed
   └─ Both clients initialize the same random board
```

#### In-Game Action Flow (Example: Draft)

```
1. Active player clicks a country to place army
   └─ GameController handles click
   └─ game.placeArmy(country) — updates local model
   └─ client.sendAction(DRAFT, "A7B2", { countryId: 15 })

2. Server receives DRAFT
   └─ broadcastToRoom("A7B2", raw payload) — pass-through

3. Other player receives DRAFT
   └─ GameController.onMessageReceived processes it
   └─ game.placeArmy(country with id 15) — mirrors the action
```

#### In-Game Attack Flow (Server-Authoritative Dice)

```
1. Attacker clicks attack
   └─ sendAction(ATTACK_REQ, "A7B2", {
        ATTACK_REQ: 15,         // source country ID
        DESTINATION_ID: 22,     // target country ID
        ATTACKER_ARMIES: 5,
        DEFENDER_ARMIES: 3
      })

2. Server receives ATTACK_REQ
   └─ Dice.rollBattle(5, 3) → BattleResult(...)
   └─ Broadcasts BATTLE_RESULT with dice rolls + losses + conquered flag

3. Both players receive BATTLE_RESULT
   └─ Apply losses to both countries
   └─ If conquered: attacker must send CONQUEST_MOVE with troop count
```

---

### 2.6 Network Package File Summary

| File | Role |
|------|------|
| `network/shared/GameAction.java` | Enum of all 16 message types |
| `network/shared/GameMessage.java` | Record: `(type, roomId, sender, content)` — the wire format |
| `network/client/RiskWebSocketClient.java` | Client-side WebSocket using `java.net.http` |
| `network/server/GameWebSocketHandler.java` | Server-side message router (Spring `TextWebSocketHandler`) |
| `network/server/RoomManager.java` | Room CRUD + session tracking + broadcast |
| `config/WebSocketConfig.java` | Registers handler at `/risk-ws` |

---

### 2.7 Key Design Decisions

1. **Server rolls dice** — prevents cheating. All other actions are trusted pass-throughs.
2. **Shared random seed** — both clients generate the same initial board without needing to send the entire board state.
3. **ngrok for NAT traversal** — avoids port forwarding hassles. The WSS URL is hardcoded in `RiskWebSocketClient`.
4. **`Platform.runLater()`** — bridges the WebSocket network thread to the JavaFX UI thread safely.
5. **`ConcurrentHashMap`** for rooms — thread-safe since WebSocket callbacks can arrive from different threads.
6. **Record-based messages** — `GameMessage` is a Java record, giving automatic serialization/deserialization with Jackson.

