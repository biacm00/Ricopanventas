# Migración Completa de Firebase a Supabase

Este plan detalla los pasos para eliminar por completo Firebase y migrar toda la lógica de la aplicación (Autenticación, Catálogo de Productos, Registro de Ventas y Cierres de Caja) a **Supabase**.

## User Review Required

> [!IMPORTANT]
> - **Estructura en Supabase:** Se requiere que en Supabase existan las tablas `usuarios`, `productos`, `ventas` y `arqueos` con las columnas correspondientes para que las consultas REST funcionen correctamente.
> - **Autenticación:** El login validará exclusivamente contra la tabla `usuarios` en Supabase mediante `SupabaseRepository.validarUsuario`.

## Open Questions

- ¿Las columnas de tus tablas en Supabase (`productos`, `ventas`, `arqueos`, `usuarios`) ya están creadas con los nombres estándar esperados por el repositorio?

## Proposed Changes

### Dependencias y Configuración
#### [MODIFY] [build.gradle.kts](file:///C:/Users/mingu/Downloads/Ricopan_ventas1/Ricopan_ventas/app/build.gradle.kts)
- Eliminar dependencias de Firebase (`firebase-auth`, `firebase-database`, `firebase-bom`).
- Eliminar el plugin `com.google.gms.google-services`.

---

### Autenticación y Login
#### [MODIFY] [LoginActivity.java](file:///C:/Users/mingu/Downloads/Ricopan_ventas1/Ricopan_ventas/app/src/main/java/com/example/ricopan_ventas/LoginActivity.java)
- Eliminar toda referencia a `FirebaseAuth` y `mAuth`.
- Validar las credenciales exclusivamente llamando a `SupabaseRepository.validarUsuario(correo, contrasena, ...)`.

---

### Módulo de Ventas y Productos
#### [MODIFY] [VentasActivity.java](file:///C:/Users/mingu/Downloads/Ricopan_ventas1/Ricopan_ventas/app/src/main/java/com/example/ricopan_ventas/VentasActivity.java)
- Reemplazar la carga desde Firebase Realtime Database (`FirebaseDatabase.getInstance().getReference("Productos")`) por `SupabaseRepository.obtenerProductos(...)`.

---

### Módulo de Cobros / Registro de Venta
#### [MODIFY] [CobroActivity.java](file:///C:/Users/mingu/Downloads/Ricopan_ventas1/Ricopan_ventas/app/src/main/java/com/example/ricopan_ventas/CobroActivity.java)
- Reemplazar `FirebaseDatabase` por `SupabaseRepository.registrarVenta(ventaData, ...)`.

---

### Módulo de Cierre de Caja
#### [MODIFY] [CierreCajaActivity.java](file:///C:/Users/mingu/Downloads/Ricopan_ventas1/Ricopan_ventas/app/src/main/java/com/example/ricopan_ventas/CierreCajaActivity.java)
- Reemplazar `FirebaseDatabase` por `SupabaseRepository.registrarArqueo(...)` y `SupabaseRepository.obtenerVentas(...)`.

## Verification Plan

### Automated Tests
- Compilación del proyecto (`./gradlew assembleDebug`) para verificar que no queden referencias a clases de Firebase no encontradas.

### Manual Verification
- Iniciar sesión con un usuario existente en la tabla `usuarios` de Supabase.
- Verificar que el catálogo muestre los productos de la tabla `productos`.
- Simular una venta en `CobroActivity` y verificar que aparezca registrada en la tabla `ventas` de Supabase.
- Realizar el cierre de caja en `CierreCajaActivity` y verificar que se guarde en la tabla `arqueos`.
