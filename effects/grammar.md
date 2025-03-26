┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃ Effects System Grammar in effects.bolt                                                                                   ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛

The effects.bolt file implements a system for defining and executing effects, triggers, and conditions in Minecraft. Here's 
a breakdown of the grammar for these types:                                                                                 


Conditions                                                                                                                  

Conditions are checks that return true or false. They have these kinds:                                                     

 • player_nearby: Checks if a player is within a specified distance                                                         
    • distance: Maximum distance to detect players                                                                          
 • is_block: Checks if a specific block exists at coordinates                                                               
    • coords: Target coordinates (default: ~ ~ ~)                                                                           
    • block: Block type to check for                                                                                        
 • all: Returns true if a condition is true for all entities with a specific tag                                            
    • at: Tag identifying entities to check                                                                                 
    • condition: The condition to check for each entity                                                                     
 • any: Returns true if a condition is true for any entity with a specific tag                                              
    • at: Tag identifying entities to check                                                                                 
    • condition: The condition to check for each entity                                                                     
 • random: Returns true based on a random chance                                                                            
    • chance: Format is "X in Y" (e.g., "1 in 4")                                                                           
 • hit_by_arrow: Returns true if hit by an arrow                                                                            
    • No additional parameters                                                                                              


Triggers                                                                                                                    

Triggers are persistent checks that can run effects when conditions change. They have these fields:                         

 • id: Unique identifier                                                                                                    
 • condition: The condition to check                                                                                        
 • forEach (optional): Tag to run this trigger for each entity with this tag                                                
 • Event handlers:                                                                                                          
    • onTrue: Effect(s) to run when condition becomes true                                                                  
    • onFalse: Effect(s) to run when condition becomes false                                                                
    • whileTrue: Effect(s) to run every tick while condition is true                                                        
    • whileFalse: Effect(s) to run every tick while condition is false                                                      


Effects                                                                                                                     

Effects are actions that can be performed. They have these common fields:                                                   

 • id: Unique identifier (can be auto-generated)                                                                            
 • kind: Type of effect                                                                                                     
 • concurrency: How multiple instances are handled                                                                          
    • one: Only one instance can run at a time                                                                              
    • perExecutor: One instance per calling entity                                                                          
    • any: Multiple instances can run simultaneously                                                                        

Effect kinds:                                                                                                               

 • sequential: Run a series of effects in order                                                                             
    • sequential: Array of effects to run in sequence                                                                       
 • parallel: Run multiple effects simultaneously                                                                            
    • parallel: Array of effects to run in parallel                                                                         
 • check_condition: Check a condition and run effects based on result                                                       
    • condition: Condition to check                                                                                         
    • onTrue: Effect to run if condition is true                                                                            
    • onFalse: Effect to run if condition is false                                                                          
 • for_each: Run an effect for each entity with a tag                                                                       
    • forEach: Tag identifying entities                                                                                     
    • effect: Effect to run for each entity                                                                                 
 • for_random_selection: Run effects on a random subset of entities                                                         
    • at: Tag identifying the pool of entities                                                                              
    • number: Number of entities to randomly select                                                                         
    • onSelected: Effect to run on selected entities                                                                        
    • onNotSelected: Effect to run on non-selected entities                                                                 
 • wait: Pause before continuing                                                                                            
    • duration: Time to wait (can include "s" suffix for seconds)                                                           
 • summon: Spawn an entity                                                                                                  
    • entity: Entity type to summon                                                                                         
    • coords (optional): Coordinates (default: ~ ~ ~)                                                                       
    • at (optional): Entity selector (default: @s)                                                                          
    • nbt (optional): NBT data for the entity                                                                               
 • tell_player: Display a message                                                                                           
    • text: Message to display                                                                                              
 • setblock: Place a block                                                                                                  
    • coords (optional): Coordinates (default: ~ ~ ~)                                                                       
    • block: Block to place                                                                                                 
 • function: Run a Minecraft function                                                                                       
    • function: Function to run                                                                                             
 • command: Run a raw Minecraft command                                                                                     
    • command: Command to execute                                                                                           
 • deactivate_effect: Disable an effect                                                                                     
    • effect_id: ID of effect to deactivate                                                                                 
 • deactivate_trigger: Disable a trigger                                                                                    
    • trigger_id: ID of trigger to deactivate                                                                               
 • activate_effect: Enable a previously deactivated effect                                                                  
    • effect_id: ID of effect to activate                                                                                   
 • activate_trigger: Enable a previously deactivated trigger                                                                
    • trigger_id: ID of trigger to activate
