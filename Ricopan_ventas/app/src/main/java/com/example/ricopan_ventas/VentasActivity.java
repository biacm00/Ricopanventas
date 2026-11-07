package com.example.ricopan_ventas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VentasActivity extends AppCompatActivity {

    // 1. VARIABLES GLOBALES DE LA PANTALLA
    Button btnCobrar, btnLimpiarCarrito, btnAgregarProducto, btnCierreCaja, btnCerrarSesion, btnImportarExcel;
    TextView tvTotal, tvTituloRol;
    LinearLayout llAdminPanel, llListaProductos;
    androidx.cardview.widget.CardView llBottomCart;
    EditText etBuscar;

    double totalPagar = 0.0;
    boolean esAdmin = false;

    static class ProductoModel {
        String id;
        String nombre;
        double precio;
        long stock;
        String categoria;
        String imagen;

        public ProductoModel(String id, String nombre, double precio, long stock, String categoria, String imagen) {
            this.id = id;
            this.nombre = nombre;
            this.precio = precio;
            this.stock = stock;
            this.categoria = categoria;
            this.imagen = imagen;
        }

        public ProductoModel(String id, String nombre, double precio, long stock, String categoria) {
            this(id, nombre, precio, stock, categoria, "");
        }
    }

    private java.util.List<ProductoModel> listaProductosCompleta = new java.util.ArrayList<>();

    // 2. MÉTODO PRINCIPAL (ÍNDICE DE FUNCIONES)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ventas);

        inicializarVistasYFirebase();
        configurarRolUsuario();
        configurarPanelAdmin();
        configurarBuscadorYFiltros();
        configurarFiltrosCategoria();
        cargarCatalogoFirebase();
        configurarBotonCobro();
        configurarBotonLimpiarCarrito();
        configurarBotonCerrarSesion();
    }

    // 3. MÉTODOS SEPARADOS (FÁCILES DE EXPLICAR)
    private void inicializarVistasYFirebase() {
        btnCobrar = findViewById(R.id.btnCobrar);
        btnLimpiarCarrito = findViewById(R.id.btnLimpiarCarrito);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        tvTotal = findViewById(R.id.tvTotal);
        tvTituloRol = findViewById(R.id.tvTituloRol);
        llAdminPanel = findViewById(R.id.llAdminPanel);
        llBottomCart = findViewById(R.id.llBottomCart);
        btnAgregarProducto = findViewById(R.id.btnAgregarProducto);
        btnImportarExcel = findViewById(R.id.btnImportarExcel);
        btnCierreCaja = findViewById(R.id.btnCierreCaja);
        llListaProductos = findViewById(R.id.llListaProductos);
        etBuscar = findViewById(R.id.etBuscar);
    }

    private void configurarBuscadorYFiltros() {
        if (etBuscar != null) {
            etBuscar.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarProductos(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void filtrarProductos(String textoBusqueda) {
        String query = textoBusqueda.toLowerCase().trim();
        java.util.List<ProductoModel> filtrados = new java.util.ArrayList<>();

        for (ProductoModel p : listaProductosCompleta) {
            if (p.nombre.toLowerCase().contains(query) || (p.categoria != null && p.categoria.toLowerCase().contains(query))) {
                filtrados.add(p);
            }
        }

        mostrarProductosEnPantalla(filtrados);
    }

    private void mostrarProductosEnPantalla(java.util.List<ProductoModel> lista) {
        llListaProductos.removeAllViews();
        for (ProductoModel p : lista) {
            agregarProductoVista(p.id, p.nombre, p.precio, p.stock, p.categoria, p.imagen);
        }
    }

    Button btnFiltroTodos, btnFiltroPanaderia, btnFiltroPasteleria, btnFiltroAbarrotes, btnFiltroCafes, btnFiltroBocaditos, btnFiltroEmbutidos, btnFiltroLacteos;

    private void configurarFiltrosCategoria() {
        btnFiltroTodos = findViewById(R.id.btnFiltroTodos);
        btnFiltroPanaderia = findViewById(R.id.btnFiltroPanaderia);
        btnFiltroPasteleria = findViewById(R.id.btnFiltroPasteleria);
        btnFiltroAbarrotes = findViewById(R.id.btnFiltroAbarrotes);
        btnFiltroCafes = findViewById(R.id.btnFiltroCafes);
        btnFiltroBocaditos = findViewById(R.id.btnFiltroBocaditos);
        btnFiltroEmbutidos = findViewById(R.id.btnFiltroEmbutidos);
        btnFiltroLacteos = findViewById(R.id.btnFiltroLacteos);

        View.OnClickListener listener = v -> {
            resetearEstilosFiltros();
            v.setBackgroundColor(Color.parseColor("#8B4513"));
            ((Button) v).setTextColor(Color.WHITE);

            int id = v.getId();
            // Clasificación precisa según lo solicitado por el usuario para la presentación
            if (id == R.id.btnFiltroPanaderia) {
                // Panadería: incluye panes, tostadas, petipán, hamburguesa, etc.
                filtrarPorCategoria("pan", "panaderia", "panadería", "frances", "tostada", "petipán", "petipan", "hamburguesa", "coliza", "caracol", "yema", "ciabatta", "carioca", "manteca", "integral", "maíz");
            } else if (id == R.id.btnFiltroPasteleria) {
                // Bocaditos y Pastelería: empanadas, alfajores, milhojas, donas, churros, cono, caramanduca
                filtrarPorCategoria("pastel", "pasteleria", "pastelería", "torta", "milhojas", "chocolate", "dulce", "bocadito", "empanada", "alfajor", "cono", "caramanduca", "dona", "churro");
            } else if (id == R.id.btnFiltroAbarrotes) {
                // Bebidas y otros abarrotes: gaseosas, agua, jugos, refrescos, azúcar, huevos, atún, papel
                filtrarPorCategoria("gaseosa", "agua", "kola real", "kr", "loa", "kris", "generade", "oro", "fruvi", "zuko", "jugo", "refresco", "azucar", "azúcar", "huevo", "atún", "papel", "conserva");
            } else if (id == R.id.btnFiltroCafes) {
                // Cafés e Infusiones: café, manzanilla, té, anís, hierba luisa, cocoa, milo
                filtrarPorCategoria("caf", "cafe", "cafés", "infusion", "infusión", "manzanilla", "té", "te", "anís", "anis", "hierba luisa", "cocoa", "milo");
            } else if (id == R.id.btnFiltroBocaditos) {
                // Se mantiene como filtro rápido de Bocaditos
                filtrarPorCategoria("bocad", "bocadito", "bocaditos", "empanada", "alfajor", "cono", "milhojas");
            } else if (id == R.id.btnFiltroEmbutidos) {
                // Embutidos: salchichas, jamonadas, paté, chorizo, hot dog, chicharrón
                filtrarPorCategoria("embut", "embutido", "salchicha", "pate", "paté", "jamonada", "mortadela", "chorizo", "hot dog", "suiza", "chicharrón", "fiambre");
            } else if (id == R.id.btnFiltroLacteos) {
                // Lácteos y Quesos: leche, yogurt, queso, mantequilla, margarina, manjar
                filtrarPorCategoria("lacteo", "lácteo", "queso", "yogurt", "yogurts", "leche", "mantequilla", "margarina", "manjar");
            }
        };

        if (btnFiltroTodos != null) {
            btnFiltroTodos.setOnClickListener(v -> {
                resetearEstilosFiltros();
                v.setBackgroundColor(Color.parseColor("#8B4513"));
                ((Button) v).setTextColor(Color.WHITE);
                mostrarProductosEnPantalla(listaProductosCompleta);
            });
        }
        if (btnFiltroPanaderia != null) btnFiltroPanaderia.setOnClickListener(listener);
        if (btnFiltroPasteleria != null) btnFiltroPasteleria.setOnClickListener(listener);
        if (btnFiltroAbarrotes != null) btnFiltroAbarrotes.setOnClickListener(listener);
        if (btnFiltroCafes != null) btnFiltroCafes.setOnClickListener(listener);
        if (btnFiltroBocaditos != null) btnFiltroBocaditos.setOnClickListener(listener);
        if (btnFiltroEmbutidos != null) btnFiltroEmbutidos.setOnClickListener(listener);
        if (btnFiltroLacteos != null) btnFiltroLacteos.setOnClickListener(listener);
    }

    private void resetearEstilosFiltros() {
        Button[] botones = {btnFiltroTodos, btnFiltroPanaderia, btnFiltroPasteleria, btnFiltroAbarrotes, btnFiltroCafes, btnFiltroBocaditos, btnFiltroEmbutidos, btnFiltroLacteos};
        for (Button b : botones) {
            if (b != null) {
                b.setBackgroundColor(Color.parseColor("#EFEBE6"));
                b.setTextColor(Color.parseColor("#333333"));
            }
        }
    }

    private void filtrarPorCategoria(String... keywords) {
        java.util.List<ProductoModel> filtrados = new java.util.ArrayList<>();
        for (ProductoModel p : listaProductosCompleta) {
            String cat = p.categoria != null ? p.categoria.toLowerCase() : "";
            String nom = p.nombre != null ? p.nombre.toLowerCase() : "";
            boolean match = false;
            for (String kw : keywords) {
                String k = kw.toLowerCase();
                if (cat.contains(k) || nom.contains(k)) {
                    match = true;
                    break;
                }
            }
            if (match) {
                filtrados.add(p);
            }
        }
        mostrarProductosEnPantalla(filtrados);
    }

    private void configurarRolUsuario() {
        String rolUsuario = getIntent().getStringExtra("TIPO_ROL");
        esAdmin = "Administrador".equals(rolUsuario);

        // Título de la pantalla
        if (rolUsuario != null) {
            tvTituloRol.setText("RICO PAN - " + rolUsuario);
        } else {
            tvTituloRol.setText("RICO PAN");
        }

        // Mostrar u ocultar controles según el rol (Administrador vs Vendedor)
        if (esAdmin) {
            llAdminPanel.setVisibility(View.VISIBLE);
            if (llBottomCart != null) {
                llBottomCart.setVisibility(View.GONE); // El administrador no simula ventas
            }
        } else {
            llAdminPanel.setVisibility(View.GONE);
            if (llBottomCart != null) {
                llBottomCart.setVisibility(View.VISIBLE); // El vendedor sí realiza ventas
            }
        }
    }

    private void configurarPanelAdmin() {
        btnAgregarProducto.setOnClickListener(v -> {
            LinearLayout contenedor = new LinearLayout(this);
            contenedor.setOrientation(LinearLayout.VERTICAL);
            contenedor.setPadding(50, 40, 50, 10);

            EditText etCodigo = new EditText(this);
            etCodigo.setHint("Código (ej. PAN01)");

            EditText etNombre = new EditText(this);
            etNombre.setHint("Nombre del producto");

            EditText etPrecio = new EditText(this);
            etPrecio.setHint("Precio (ej. 6.50)");
            etPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

            EditText etStock = new EditText(this);
            etStock.setHint("Cantidad en Stock (ej. 50)");
            etStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

            // Selector de Categoría
            TextView tvCat = new TextView(this);
            tvCat.setText("Categoría:");
            tvCat.setPadding(0, 20, 0, 0);

            android.widget.Spinner spinnerCategoria = new android.widget.Spinner(this);
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Panadería", "Pastelería", "Abarrotes", "Cafés e Infusiones"});
            spinnerCategoria.setAdapter(adapter);

            contenedor.addView(etCodigo);
            contenedor.addView(etNombre);
            contenedor.addView(etPrecio);
            contenedor.addView(etStock);
            contenedor.addView(tvCat);
            contenedor.addView(spinnerCategoria);

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Nuevo Producto")
                    .setView(contenedor)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        String codigo = etCodigo.getText().toString().toUpperCase();
                        String nombre = etNombre.getText().toString();
                        String precioStr = etPrecio.getText().toString();
                        String stockStr = etStock.getText().toString();
                        String categoria = spinnerCategoria.getSelectedItem().toString();

                        if (!codigo.isEmpty() && !nombre.isEmpty() && !precioStr.isEmpty()) {
                            double precio = Double.parseDouble(precioStr);
                            long stock = stockStr.isEmpty() ? 50 : Long.parseLong(stockStr);

                            // Guardar en Supabase
                            Map<String, Object> producto = new HashMap<>();
                            producto.put("codigo", codigo);
                            producto.put("nombre", nombre);
                            producto.put("precio", precio);
                            producto.put("stock", stock);
                            producto.put("categoria", categoria);

                            SupabaseRepository.insertarProducto(producto, new SupabaseRepository.SupabaseCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {
                                    Toast.makeText(VentasActivity.this, "Producto guardado", Toast.LENGTH_SHORT).show();
                                    cargarCatalogoFirebase();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Toast.makeText(VentasActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Lógica de importación de Inventario desde Excel (CSV)
        btnImportarExcel.setOnClickListener(v -> {
            LinearLayout contenedor = new LinearLayout(this);
            contenedor.setOrientation(LinearLayout.VERTICAL);
            contenedor.setPadding(40, 20, 40, 10);

            TextView tvInstrucciones = new TextView(this);
            tvInstrucciones.setText("Pegue las líneas CSV del inventario Excel:\n(Formato: Código, Nombre, Precio, Categoría, Stock)");
            tvInstrucciones.setTextSize(13);
            tvInstrucciones.setTextColor(Color.parseColor("#333333"));

            EditText etCsvData = new EditText(this);
            etCsvData.setLines(8);
            etCsvData.setGravity(Gravity.TOP);
            etCsvData.setText("PAN01, Milhojas con Manjar Blanco, 5.00, Pastelería, 50\nPAN02, Pan Francés Docena, 4.50, Panadería, 100\nPAN03, Empanada de Carne, 6.00, Panadería, 30\nBEB01, Café Americano 8oz, 4.00, Cafés e Infusiones, 40\nTORT01, Torta de Chocolate 1K, 45.00, Pastelería, 15");

            contenedor.addView(tvInstrucciones);
            contenedor.addView(etCsvData);

            new AlertDialog.Builder(this)
                    .setTitle("Importar Inventario Excel (CSV)")
                    .setView(contenedor)
                    .setPositiveButton("Cargar Inventario", (dialog, which) -> {
                        String csvText = etCsvData.getText().toString();
                        importarInventarioCsv(csvText);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Botón para ir al Cierre de Caja
        btnCierreCaja.setOnClickListener(v -> {
            Intent intentCierre = new Intent(VentasActivity.this, CierreCajaActivity.class);
            startActivity(intentCierre);
        });
    }

    private void importarInventarioCsv(String csvData) {
        if (csvData == null || csvData.trim().isEmpty()) {
            Toast.makeText(this, "Datos de inventario vacíos", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] lineas = csvData.split("\n");
        int cargados = 0;

        for (String linea : lineas) {
            String[] partes = linea.split(",");
            if (partes.length >= 4) {
                String codigo = partes[0].trim();
                String nombre = partes[1].trim();
                double precio = Double.parseDouble(partes[2].trim());
                String categoria = partes[3].trim();
                long stock = (partes.length >= 5) ? Long.parseLong(partes[4].trim()) : 50;

                Map<String, Object> producto = new HashMap<>();
                producto.put("codigo", codigo);
                producto.put("nombre", nombre);
                producto.put("precio", precio);
                producto.put("categoria", categoria);
                producto.put("stock", stock);

                SupabaseRepository.insertarProducto(producto, new SupabaseRepository.SupabaseCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {}
                    @Override public void onError(String errorMessage) {}
                });
                cargados++;
            }
        }

        Toast.makeText(this, "¡Inventario importado a Supabase!", Toast.LENGTH_LONG).show();
        cargarCatalogoFirebase();
    }

    private void cargarInventarioBase() {
        listaProductosCompleta.clear();
        listaProductosCompleta.add(new ProductoModel("1", "Salchicha de pollo (3 unidades)", 1.50, 30, "Salchichas"));
        listaProductosCompleta.add(new ProductoModel("2", "Salchicha de pollo (6 unidades)", 3.00, 30, "Salchichas"));
        listaProductosCompleta.add(new ProductoModel("3", "Paté", 3.00, 30, "Patés"));
        listaProductosCompleta.add(new ProductoModel("4", "Jamonada de pavita", 2.50, 30, "Jamonadas"));
        listaProductosCompleta.add(new ProductoModel("5", "Jamonada de pollo", 1.70, 30, "Jamonadas"));
        listaProductosCompleta.add(new ProductoModel("6", "Jamonada Mortadela", 2.00, 30, "Mortadelas"));
        listaProductosCompleta.add(new ProductoModel("7", "Chorizo", 2.00, 30, "Chorizos"));
        listaProductosCompleta.add(new ProductoModel("8", "Yogurt Griego Frutos Rojos", 2.00, 30, "Yogurts"));
        listaProductosCompleta.add(new ProductoModel("9", "Yogurt Griego Arándanos", 2.00, 30, "Yogurts"));
        listaProductosCompleta.add(new ProductoModel("10", "Queso Edam Tajadas Finas", 5.50, 30, "Quesos"));
        listaProductosCompleta.add(new ProductoModel("11", "Hot Dog Suiza", 5.50, 30, "Hot Dogs"));
        listaProductosCompleta.add(new ProductoModel("12", "Milhojas con Manjar Blanco", 5.00, 50, "Pastelería"));
        listaProductosCompleta.add(new ProductoModel("13", "Pan Francés (Docena)", 4.50, 100, "Panadería"));
        listaProductosCompleta.add(new ProductoModel("14", "Empanada de Carne", 6.00, 30, "Panadería"));
        listaProductosCompleta.add(new ProductoModel("15", "Café Americano 8oz", 4.00, 40, "Cafés e Infusiones"));
        listaProductosCompleta.add(new ProductoModel("16", "Torta de Chocolate 1K", 45.00, 15, "Pastelería"));
        mostrarProductosEnPantalla(listaProductosCompleta);
    }

    private void cargarCatalogoFirebase() {
        // Cargar desde Supabase REST API
        SupabaseRepository.obtenerProductos(new SupabaseRepository.SupabaseCallback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray result) {
                listaProductosCompleta.clear();
                if (result != null && result.size() > 0) {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject item = result.get(i).getAsJsonObject();
                        String id = item.has("id") && !item.get("id").isJsonNull() ? item.get("id").getAsString() : String.valueOf(i);

                        // Nombre / Producto
                        String nombre = "Producto";
                        if (item.has("Producto") && !item.get("Producto").isJsonNull()) {
                            nombre = item.get("Producto").getAsString();
                        } else if (item.has("nombre") && !item.get("nombre").isJsonNull()) {
                            nombre = item.get("nombre").getAsString();
                        }

                        // Precio
                        double precio = 0.0;
                        if (item.has("Precio") && !item.get("Precio").isJsonNull()) {
                            precio = parsePrecio(item.get("Precio"));
                        } else if (item.has("precio") && !item.get("precio").isJsonNull()) {
                            precio = parsePrecio(item.get("precio"));
                        }

                        // Stock
                        long stock = 30;
                        if (item.has("Stock") && !item.get("Stock").isJsonNull()) {
                            stock = item.get("Stock").getAsLong();
                        } else if (item.has("stock") && !item.get("stock").isJsonNull()) {
                            stock = item.get("stock").getAsLong();
                        }

                        // Categoría
                        String categoria = "";
                        if (item.has("Categoría") && !item.get("Categoría").isJsonNull()) {
                            categoria = item.get("Categoría").getAsString();
                        } else if (item.has("categoria") && !item.get("categoria").isJsonNull()) {
                            categoria = item.get("categoria").getAsString();
                        }

                        // Imagen / Thumbnail
                        String imagen = "";
                        if (item.has("imagen") && !item.get("imagen").isJsonNull()) {
                            imagen = item.get("imagen").getAsString();
                        } else if (item.has("thumbnail") && !item.get("thumbnail").isJsonNull()) {
                            imagen = item.get("thumbnail").getAsString();
                        }

                        listaProductosCompleta.add(new ProductoModel(id, nombre, precio, stock, categoria, imagen));
                    }
                    mostrarProductosEnPantalla(listaProductosCompleta);
                } else {
                    cargarInventarioBase();
                }
            }

            @Override
            public void onError(String errorMessage) {
                cargarInventarioBase();
            }
        });
    }

    private void configurarBotonCobro() {
        btnCobrar.setOnClickListener(v -> {
            if (totalPagar > 0) {
                Intent intentCobro = new Intent(VentasActivity.this, CobroActivity.class);
                intentCobro.putExtra("TOTAL_VENTA", totalPagar);
                startActivity(intentCobro);

                // Reiniciamos el carrito para la próxima venta
                totalPagar = 0.0;
                tvTotal.setText("S/. 0.00");
            } else {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarBotonLimpiarCarrito() {
        if (btnLimpiarCarrito != null) {
            btnLimpiarCarrito.setOnClickListener(v -> {
                if (totalPagar > 0) {
                    new AlertDialog.Builder(this)
                            .setTitle("Limpiar Carrito")
                            .setMessage("¿Deseas vaciar el carrito y reiniciar la venta actual?")
                            .setPositiveButton("Sí, limpiar", (dialog, which) -> {
                                totalPagar = 0.0;
                                tvTotal.setText("S/. 0.00");
                                Toast.makeText(this, "Carrito vaciado", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                } else {
                    Toast.makeText(this, "El carrito ya está vacío", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ==========================================
    // 4. GENERADOR VISUAL (Dibuja los productos con imagen real o emoji de respaldo)
    // ==========================================
    private void agregarProductoVista(String idProducto, String nombre, double precio, long stock, String categoria, String imagen) {
        // Contenedor principal de la tarjeta de producto (CardView)
        androidx.cardview.widget.CardView cardContainer = new androidx.cardview.widget.CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        cardContainer.setLayoutParams(cardParams);
        cardContainer.setRadius(28); // Bordes redondeados
        cardContainer.setCardElevation(6);
        cardContainer.setCardBackgroundColor(Color.WHITE);

        // Layout interno horizontal para alinear imagen y textos
        LinearLayout card = new LinearLayout(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.setPadding(24, 20, 24, 20);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        // --- GESTIÓN DE IMAGEN DEL PRODUCTO (URL real o Icono de respaldo) ---
        if (imagen != null && !imagen.trim().isEmpty() && imagen.startsWith("http")) {
            // Si el producto tiene una URL de imagen válida (como la del Excel), creamos un ImageView
            ImageView ivProduct = new ImageView(this);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(110, 110);
            imgParams.setMargins(0, 0, 20, 0);
            ivProduct.setLayoutParams(imgParams);
            ivProduct.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.addView(ivProduct);

            // Carga asíncrona de la imagen en segundo plano para no congelar la app
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL(imagen);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    if (bitmap != null) {
                        runOnUiThread(() -> ivProduct.setImageBitmap(bitmap));
                    }
                } catch (Exception e) {
                    // Si falla la red, no hacemos nada y se queda en blanco
                }
            }).start();
        } else {
            // Si no hay URL, mostramos un icono/emoji representativo según su categoría
            TextView tvThumb = new TextView(this);
            String icono = "🍞";
            String catL = (categoria != null ? categoria.toLowerCase() : "");
            if (catL.contains("pastel") || catL.contains("torta") || catL.contains("dulce")) icono = "🍰";
            else if (catL.contains("abarrote") || catL.contains("azúcar") || catL.contains("azucar")) icono = "🛒";
            else if (catL.contains("café") || catL.contains("cafe") || catL.contains("bebida") || catL.contains("infusión")) icono = "☕";
            else if (catL.contains("bocad")) icono = "🥐";
            else if (catL.contains("chorizo") || catL.contains("embutido") || catL.contains("jamonada") || catL.contains("salchicha")) icono = "🌭";
            else if (catL.contains("queso") || catL.contains("lacteo") || catL.contains("yogurt")) icono = "🧀";

            tvThumb.setText(icono);
            tvThumb.setTextSize(26);
            tvThumb.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(110, 110);
            thumbParams.setMargins(0, 0, 20, 0);
            tvThumb.setLayoutParams(thumbParams);
            card.addView(tvThumb);
        }

        // Layout vertical para Nombre, Precio y Stock del producto
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre);
        tvNombre.setTextSize(16);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setTextColor(Color.parseColor("#222222"));
        textLayout.addView(tvNombre);

        TextView tvPrecio = new TextView(this);
        tvPrecio.setText(String.format(Locale.getDefault(), "S/. %.2f", precio));
        tvPrecio.setTextSize(15);
        tvPrecio.setTextColor(Color.parseColor("#8B4513"));
        tvPrecio.setTypeface(null, Typeface.BOLD);
        textLayout.addView(tvPrecio);

        TextView tvStock = new TextView(this);
        if (stock > 0) {
            tvStock.setText("Stock: " + stock + " und.");
            tvStock.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvStock.setText("¡AGOTADO!");
            tvStock.setTextColor(Color.parseColor("#D32F2F"));
            tvStock.setTypeface(null, Typeface.BOLD);
        }
        tvStock.setTextSize(12);
        textLayout.addView(tvStock);

        card.addView(textLayout);

        // Si el usuario es Administrador, agregamos el botón de eliminar producto
        if (esAdmin) {
            Button btnEliminar = new Button(this);
            btnEliminar.setText("🗑️");
            btnEliminar.setTextColor(Color.WHITE);
            btnEliminar.setBackgroundColor(Color.parseColor("#D32F2F"));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 0, 10, 0);
            btnEliminar.setLayoutParams(btnParams);

            btnEliminar.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Eliminar Producto")
                    .setMessage("¿Estás seguro de eliminar '" + nombre + "' del catálogo?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        SupabaseRepository.eliminarProducto(idProducto, new SupabaseRepository.SupabaseCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                Toast.makeText(VentasActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                                cargarCatalogoFirebase();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(VentasActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show());

            card.addView(btnEliminar);
        }

        Button btnAdd = new Button(this);
        btnAdd.setText("+");
        btnAdd.setTextSize(18);
        btnAdd.setTypeface(null, Typeface.BOLD);
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setBackgroundColor(stock > 0 ? Color.parseColor("#8B4513") : Color.GRAY);
        btnAdd.setEnabled(stock > 0);

        // Sumar al carrito al presionar "+" con lógica promocional para Panes y Bocaditos
        btnAdd.setOnClickListener(v -> {
            String nombreLower = nombre.toLowerCase();
            String catLower = (categoria != null ? categoria.toLowerCase() : "");

            if (nombreLower.contains("pan") || catLower.contains("panadería") || catLower.contains("panaderia")) {
                mostrarDialogoPromocionPanes(nombre);
            } else if (nombreLower.contains("bocad") || catLower.contains("bocadito") || catLower.contains("bocaditos")) {
                mostrarDialogoPromocionBocaditos(nombre);
            } else {
                totalPagar += precio;
                tvTotal.setText(String.format(Locale.getDefault(), "S/. %.2f", totalPagar));
                Toast.makeText(this, nombre + " agregado", Toast.LENGTH_SHORT).show();
            }
        });

        card.addView(btnAdd);
        cardContainer.addView(card);
        llListaProductos.addView(cardContainer);
    }

    private void configurarBotonCerrarSesion() {
        btnCerrarSesion.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro de que deseas salir?")
                    .setPositiveButton("Sí, salir", (dialog, which) -> {
                        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(VentasActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void mostrarDialogoPromocionPanes(String nombre) {
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setPadding(40, 30, 40, 10);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Promoción: 4 Panes por S/. 1.00 (o S/. 0.30 c/u)\nSelecciona cantidad o monto rápido:");
        tvInfo.setTextSize(13);

        EditText etCantidad = new EditText(this);
        etCantidad.setHint("Ingrese cantidad de panes (ej. 5)");
        etCantidad.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        contenedor.addView(tvInfo);
        contenedor.addView(etCantidad);

        new AlertDialog.Builder(this)
                .setTitle("Venta Rápida - " + nombre)
                .setView(contenedor)
                .setNeutralButton("4 Panes (S/. 1.00)", (d, w) -> agregarAlCarritoPromocion(nombre, 4, 1.00))
                .setNegativeButton("8 Panes (S/. 2.00)", (d, w) -> agregarAlCarritoPromocion(nombre, 8, 2.00))
                .setPositiveButton("Calcular Cantidad", (d, w) -> {
                    String cantStr = etCantidad.getText().toString().trim();
                    if (!cantStr.isEmpty()) {
                        int cant = Integer.parseInt(cantStr);
                        double totalPromo = calcularPrecioPanes(cant);
                        agregarAlCarritoPromocion(nombre, cant, totalPromo);
                    }
                })
                .show();
    }

    private void mostrarDialogoPromocionBocaditos(String nombre) {
        LinearLayout contenedor = new LinearLayout(this);
        contenedor.setOrientation(LinearLayout.VERTICAL);
        contenedor.setPadding(40, 30, 40, 10);

        TextView tvInfo = new TextView(this);
        tvInfo.setText("Promoción: 3 Bocaditos por S/. 1.00 (o S/. 0.40 c/u)\nIngrese la cantidad:");
        tvInfo.setTextSize(13);

        EditText etCantidad = new EditText(this);
        etCantidad.setHint("Ingrese cantidad (ej. 4)");
        etCantidad.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        contenedor.addView(tvInfo);
        contenedor.addView(etCantidad);

        new AlertDialog.Builder(this)
                .setTitle("Venta Rápida - " + nombre)
                .setView(contenedor)
                .setNeutralButton("3 Bocaditos (S/. 1.00)", (d, w) -> agregarAlCarritoPromocion(nombre, 3, 1.00))
                .setPositiveButton("Calcular Cantidad", (d, w) -> {
                    String cantStr = etCantidad.getText().toString().trim();
                    if (!cantStr.isEmpty()) {
                        int cant = Integer.parseInt(cantStr);
                        double totalPromo = calcularPrecioBocaditos(cant);
                        agregarAlCarritoPromocion(nombre, cant, totalPromo);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private double calcularPrecioPanes(int cantidad) {
        int paquetes = cantidad / 4;
        int sueltos = cantidad % 4;
        return (paquetes * 1.00) + (sueltos * 0.30);
    }

    private double calcularPrecioBocaditos(int cantidad) {
        int paquetes = cantidad / 3;
        int sueltos = cantidad % 3;
        return (paquetes * 1.00) + (sueltos * 0.40);
    }

    private void agregarAlCarritoPromocion(String nombre, int cantidad, double total) {
        totalPagar += total;
        tvTotal.setText(String.format(Locale.getDefault(), "S/. %.2f", totalPagar));
        Toast.makeText(this, cantidad + "x " + nombre + " agregado (S/. " + String.format(Locale.getDefault(), "%.2f", total) + ")", Toast.LENGTH_LONG).show();
    }

    private double parsePrecio(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) return 0.0;
        try {
            return element.getAsDouble();
        } catch (Exception e) {
            String raw = element.getAsString();
            String clean = raw.replace("S/.", "").replace("S/", "").replace(",", ".").trim();
            StringBuilder sb = new StringBuilder();
            boolean foundDot = false;
            for (char c : clean.toCharArray()) {
                if (Character.isDigit(c)) {
                    sb.append(c);
                } else if (c == '.' && !foundDot) {
                    sb.append(c);
                    foundDot = true;
                }
            }
            try {
                return Double.parseDouble(sb.toString());
            } catch (Exception ex) {
                return 0.0;
            }
        }
    }
}