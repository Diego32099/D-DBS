# D-DBS
Diego's Dynamic Bullet System (DDBS)
A custom, high-performance bullet simulation system for Minecraft mods. Designed to replace traditional projectile entities with lightweight, server-side logic.

Features:
Fully tick-based simulation with customizable physics (gravity, drag, speed).

Object pool supporting a large number of active bullets simultaneously.

No physical entities — all bullets are simulated as data-only objects.

Seamless integration with the Point Blank mod: intercepts weapon fire events and reads ammo data from AmmoConfig.

Supports multi-projectile firing, armor penetration, damage falloff, and spread (in progress).

To Do:
Add custom impact logic using byte data on AmmoConfig and switch-case.
Example: byte = 1, case 1: { IncendiaryDamageLogic }

For modpacks or servers that need realistic ballistics without the performance cost of entity-based systems.
