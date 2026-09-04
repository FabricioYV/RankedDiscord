# RankedDiscord

Plugin de Minecraft (Bukkit/Spigot/Paper) que conecta el servidor con Discord: verificación de cuentas, comandos administrativos vía Discord, y mensajes de bienvenida personalizados.

Trabaja junto a [RankedMinecraft](https://github.com/FabricioYV/RankedMinecraft) (matchmaking, ELO/MMR), compartiendo la misma base de datos MySQL en la tabla `ranked_players`. **Para instalar el sistema completo (ambos plugins), seguí la [guía de instalación](https://github.com/FabricioYV/RankedMinecraft/blob/master/SETUP.md) en el repo de RankedMinecraft.**

## Qué hace

- `/verify` (en el juego): genera un código de verificación que vincula la cuenta de Minecraft con Discord.
- `/setwelcome` (en el juego): permite a jugadores elegibles personalizar su mensaje de bienvenida.
- Bot de Discord con comandos de prefijo (`!ip`, `!info`, `!instrucciones5v5`, `!instrucciones8v8`, `!donacion`, `!stats`) y comandos de administrador (ajustar ELO/wins/losses, resetear estadísticas).
- Mensaje de bienvenida al primer join de un jugador.

## Configuración

Al arrancar por primera vez se genera `plugins/RankedDiscord/config.yml`. Completá:

- `database.host/port/database/username/password` — la **misma base de datos** que usa RankedMinecraft.
- `discord.token` — token del bot (Discord Developer Portal).
- `discord.queue_role_name` — nombre del rol `@Queue` en tu servidor de Discord.
- `discord.super-admin-user-id` — tu ID de usuario de Discord, único autorizado para `/resetallstats`.

Si dejás algún campo con su placeholder `PUT_..._HERE` de fábrica, el plugin te avisa en consola exactamente qué falta y se deshabilita solo, en vez de fallar con errores confusos de conexión.

## Build

```bash
mvn clean package
```

## Licencia

GPL-3.0 — ver [LICENSE](LICENSE).
