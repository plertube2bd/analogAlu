================================================================================
ANALOG REDSTONE ALU
================================================================================
Minecraft Fabric mod that provides 4-bit analog redstone computing blocks.
Signal strength (0-15) is treated directly as unsigned 4-bit integer data.

Mod ID          : analog_alu
Display Name    : Analog Redstone ALU
Version         : 0.1.0
Description     : 4-bit analog redstone computing blocks
Minecraft       : >= 26.1
Fabric Loader   : >= 0.19.3
Fabric API      : required
Java            : 25


================================================================================
1. CORE CONCEPTS
================================================================================

- Data representation
  Redstone signal strength 0..15 == unsigned 4-bit integer value 0..15.
  All arithmetic results are masked with & 15 (mod 16).

- Block shape
  Most logic blocks are flat (2-pixel tall), similar to a redstone repeater.
  CRAM is a full cube.

- Orientation (FACING)
  FACING = output direction (front).
  When the player places the block, FACING is set to the direction the player
  is looking. Signal therefore flows away from the player.

- Input sides
  Dual-input blocks (Adder, Subtractor, OR/AND/XOR):
    A = left side relative to FACING
    B = right side relative to FACING
  Single-input blocks (Extender, Shift Left/Right, Bitwise NOT, Logical NOT):
    Input = back side (opposite of FACING)

- Outputs
  Main result  : front side (signal strength = computed value 0-15)
  Carry/Borrow : back side (signal strength 0 or 1), only on Adder/Subtractor

- Timing
  Input changes are scheduled; result is applied after 1 game tick.
  Multiple changes in the same tick are coalesced into a single computation.


================================================================================
2. BLOCK REFERENCE
================================================================================

--------------------------------------------------------------------------------
2.1 Copper Wire
--------------------------------------------------------------------------------
ID          : analog_alu:copper_wire
Purpose     : Propagates analog signal strength without attenuation.
Range       : Up to 32 blocks along a connected path.
Notes       :
  - Behaves like redstone dust for connection geometry (side / up).
  - Can connect to vanilla redstone wire, repeaters, observers, and other
    signal sources.
  - Acts as a new power source for the next copper-wire segment when driven
    by an Extender.
Crafting    : 8 redstone + 1 copper nugget -> 8 copper wire

--------------------------------------------------------------------------------
2.2 Redstone Extender
--------------------------------------------------------------------------------
ID          : analog_alu:extender
Inputs      : back
Output      : front = input value (1-tick delay)
Purpose     : Repeats the exact signal strength and resets copper-wire distance
              counter, allowing another 32 blocks of lossless transmission.
Special     : Output 0 when input is 0.

--------------------------------------------------------------------------------
2.3 Adder
--------------------------------------------------------------------------------
ID          : analog_alu:adder
Inputs      : A (left), B (right)
Outputs     :
  front = (A + B) mod 16
  back  = Carry (1 if A + B >= 16, else 0)
Interaction : Right-click toggles Carry output enable/disable.

--------------------------------------------------------------------------------
2.4 Subtractor
--------------------------------------------------------------------------------
ID          : analog_alu:subtractor
Inputs      : A (left), B (right)
Outputs     :
  front = (A - B) mod 16
  back  = Borrow (1 if A < B, else 0)
Interaction : Right-click toggles Borrow output enable/disable.

--------------------------------------------------------------------------------
2.5 Bit Shift Right (SHR)
--------------------------------------------------------------------------------
ID          : analog_alu:shift_right
Input       : back
Output      : front = A >> 1

--------------------------------------------------------------------------------
2.6 Bit Shift Left (SHL)
--------------------------------------------------------------------------------
ID          : analog_alu:shift_left
Input       : back
Output      : front = (A << 1) mod 16

--------------------------------------------------------------------------------
2.7 OR Gate
--------------------------------------------------------------------------------
ID          : analog_alu:or_gate
Inputs      : A (left), B (right)
Output      : front = A | B   (or ~(A | B) & 15 when inverted)
Interaction : Right-click toggles inverted mode (NOR).

--------------------------------------------------------------------------------
2.8 AND Gate
--------------------------------------------------------------------------------
ID          : analog_alu:and_gate
Inputs      : A (left), B (right)
Output      : front = A & B   (or ~(A & B) & 15 when inverted)
Interaction : Right-click toggles inverted mode (NAND).

--------------------------------------------------------------------------------
2.9 XOR Gate
--------------------------------------------------------------------------------
ID          : analog_alu:xor_gate
Inputs      : A (left), B (right)
Output      : front = A ^ B   (or ~(A ^ B) & 15 when inverted)
Interaction : Right-click toggles inverted mode (XNOR).

--------------------------------------------------------------------------------
2.10 Bitwise NOT
--------------------------------------------------------------------------------
ID          : analog_alu:bitwise_not
Input       : back
Output      : front = (~A) & 15
Example     : 6 (0110) -> 9 (1001)

--------------------------------------------------------------------------------
2.11 Logical NOT
--------------------------------------------------------------------------------
ID          : analog_alu:logical_not
Input       : back
Output      : front = 15 if A == 0, else 0

--------------------------------------------------------------------------------
2.12 CRAM (4-bit Memory)
--------------------------------------------------------------------------------
ID          : analog_alu:cram
Shape       : full cube
Storage     : value stored in POWER block-state property (persists with world)

Port layout (top-down view, front = north in this description):
  - Front  : data output (stored value)
  - Left   : D (clear / reset)
  - Back   : I (data input)
  - Other faces unused

Write logic (evaluated every tick):
  if D > 0          -> store 0          (clear has priority)
  else if I > 0     -> store I
  else              -> keep previous value

Placement   : can be placed on any solid surface (no special support check).


================================================================================
3. INTERACTION SUMMARY
================================================================================

Right-click (empty hand, mayBuild permission required):
  - Adder / Subtractor : toggle CARRY_ENABLED
  - OR / AND / XOR     : toggle INVERTED (NOR / NAND / XNOR)

Placement rules:
  - Flat logic blocks require a sturdy face below or a hopper.
  - CRAM has no special placement restriction.


================================================================================
4. SOURCE CODE STRUCTURE
================================================================================

com.example.analogalu
├── AnalogAlu.java                 # ModInitializer, creative-tab registration
├── ModBlocks.java                 # Block & BlockItem registration
└── block/
    ├── AluBlock.java              # Abstract base for all logic blocks
    │                              #   - FACING, POWER, CARRY, CARRY_ENABLED, INVERTED
    │                              #   - inputA / inputB / inputBack helpers
    │                              #   - 1-tick scheduled update
    │                              #   - signal source implementation
    ├── AluBlocks.java             # Concrete implementations:
    │                              #   OrGate, AndGate, XorGate,
    │                              #   BitwiseNot, LogicalNot,
    │                              #   Adder, Subtractor,
    │                              #   Extender, ShiftLeft, ShiftRight,
    │                              #   Cram
    └── CopperWireBlock.java       # Lossless analog wire (max distance 32)

Key constants:
  AluBlock.DELAY_TICKS     = 1
  CopperWireBlock.MAX_DISTANCE = 32
  CopperWireBlock.MAX_NETWORK  = 4096   (safety limit)


================================================================================
5. BUILD & ASSETS
================================================================================

gradle.properties keys (replace with values from https://fabricmc.net/develop):
  minecraft_version
  loader_version
  loom_version
  fabric_api_version
  mod_version
  maven_group
  archives_base_name

Build command:
  ./gradlew build

Output:
  build/libs/analog-alu-<version>.jar

Asset generation:
  tools/gen_alu_assets.py
  Generates textures, block models, blockstates, recipes, loot tables,
  and language entries for the ALU blocks.


================================================================================
6. CRAFTING RECIPES (HIGH LEVEL)
================================================================================

Copper Wire     : redstone + copper nugget  (shaped, yields 8)
Extender        : copper ingot + redstone + stone-like
Adder           : copper ingot + redstone + stone-like
Subtractor      : copper ingot + redstone torch + redstone + stone-like
OR / AND / XOR  : copper ingot + redstone / redstone torch + stone-like
Logical NOT     : redstone torch + stone-like
Bitwise NOT     : Logical NOT + copper ingot  (shapeless)
Shift Left/Right: copper ingot + redstone + stone-like
CRAM            : redstone + copper ingot + stone-like

Exact patterns are in:
  src/main/resources/data/analog_alu/recipe/


================================================================================
7. LANGUAGE FILES
================================================================================

en_us.json and ko_kr.json provide display names for all blocks.
English names include interaction hints where relevant


================================================================================
8. WAFER PROGRAM LANGUAGE
================================================================================

Programs start with one module declaration, then declare all four ports and
storage before executable statements:

  module main;
  port front: 4;
  port back: 4;
  port left: 4;
  port right: 4;
  cram scratch: 256;   // CRAM capacity in bytes
  ff flags: 2048;      // FF capacity in bits
  let a = 1;
  let b = 5;
  let total = a ++ b;
  let carry = total.carry;
  show(a ++ b);
  front = +(a, b);

CRAM bytes plus each FF array's bit count rounded up to bytes must not exceed
512 per semiconductor. Each statement, including declarations, costs 1 logical
millisecond. Arithmetic is 4-bit: + and - discard carry/borrow; ++ returns
(result, carry), while -- returns (result, borrow). Use .carry or .borrow to
read the status bit. CRAM and FF arrays are read and written with [index].
Operators can also be called as functions, for example +(a, b) and ++(a, b).
Shift operators are << and >>; <<< and >>> rotate left and right respectively.

Saving a wafer compiles the source to versioned WBC1 stack bytecode and stores
both source and bytecode. The bytecode is copied through crafting, furnace
processing, and the mass analyzer. Shift-use a programmed wafer or finished
semiconductor to execute the compiled program; show(...) output and the final
front value are sent to chat. Item execution starts all four ports at 0.
Execution is atomic on the server; the 1 ms statement cost is logical program
time, since Minecraft's world updates are tick-based.
(e.g. "OR Gate (right-click: NOR)").


================================================================================
END OF README
================================================================================
