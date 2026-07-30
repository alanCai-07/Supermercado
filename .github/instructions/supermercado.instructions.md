---
name: supermercado-instructions
description: "Repository-level agent instructions for reviewing, correcting, and finalizing the Supermercado facturación system. Use when modifying project code, documentation, configuration, or build setup."
applyTo:
  - "**/*.java"
  - "**/*.md"
  - "**/*.properties"
  - "**/*.xml"
  - "**/*.sql"
---

Sigue estas reglas cuando trabajes en este repositorio:

- Prioriza estabilidad, funcionalidad y cumplimiento de los requerimientos presentes en la documentación del proyecto.
- Antes de realizar cambios, analiza la arquitectura actual, los archivos de configuración (`config.properties`, `pom.xml`) y la documentación (`README.md`, `README-configuracion.md`).
- Corrige errores existentes sin inventar nuevas funcionalidades. Todo cambio debe resolver un problema real o mejorar la calidad del código.
- Mantén coherencia con el estilo actual del proyecto y evita sobreingeniería.
- Refactoriza solo cuando mejore legibilidad, organización o mantenibilidad.
- Mejora validaciones, manejo de errores y experiencia de usuario dentro de la lógica ya implementada.
- Preserva el idioma español en mensajes, etiquetas y documentación cuando ya está usado en el proyecto.
- No agregues credenciales, secretos ni configuraciones sensibles al repositorio.
- Al proponer cambios, usa un formato técnico y directo con los siguientes puntos: problema detectado, causa, solución, código corregido y resultado esperado.
- Siempre verifica que el proyecto pueda ejecutarse correctamente con Maven después de las modificaciones.
