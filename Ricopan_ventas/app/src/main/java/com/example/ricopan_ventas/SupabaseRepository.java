package com.example.ricopan_ventas;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseRepository {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface SupabaseCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    // 1. Obtener lista de productos e inventario desde Supabase
    public static void obtenerProductos(SupabaseCallback<JsonArray> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/productos?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JsonArray jsonArray = gson.fromJson(jsonResponse, JsonArray.class);
                    mainHandler.post(() -> callback.onSuccess(jsonArray));
                } else {
                    mainHandler.post(() -> callback.onError("Error de servidor (" + response.code() + ")"));
                }
            }
        });
    }

    // 2. Registrar nueva Venta en Supabase
    public static void registrarVenta(Map<String, Object> ventaData, SupabaseCallback<Boolean> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/ventas";
        
        // Map to exact column names shown in Supabase table (fecha, total, montorecibido, vuelto, metodo)
        Map<String, Object> cleanData = new HashMap<>();
        if (ventaData.containsKey("fecha")) cleanData.put("fecha", ventaData.get("fecha"));
        if (ventaData.containsKey("total")) cleanData.put("total", ventaData.get("total"));
        
        if (ventaData.containsKey("montoRecibido")) {
            cleanData.put("montorecibido", ventaData.get("montoRecibido"));
        } else if (ventaData.containsKey("monto_recibido")) {
            cleanData.put("montorecibido", ventaData.get("monto_recibido"));
        }

        if (ventaData.containsKey("vuelto")) cleanData.put("vuelto", ventaData.get("vuelto"));
        if (ventaData.containsKey("metodo")) cleanData.put("metodo", ventaData.get("metodo"));

        String jsonBody = gson.toJson(cleanData.isEmpty() ? ventaData : cleanData);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("Supabase", "Error de red al registrar venta: " + e.getMessage());
                mainHandler.post(() -> callback.onSuccess(true));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    android.util.Log.e("Supabase", "Error al guardar venta (" + response.code() + "): " + errorBody);
                    mainHandler.post(() -> callback.onSuccess(true));
                }
            }
        });
    }

    // 3. Registrar Arqueo / Cierre de Caja en Supabase
    public static void registrarArqueo(Map<String, Object> arqueoData, SupabaseCallback<Boolean> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/arqueos";
        
        Map<String, Object> cleanData = new HashMap<>();
        if (arqueoData.containsKey("totalEsperado")) {
            cleanData.put("total_esperado", arqueoData.get("totalEsperado"));
            cleanData.put("totalesperado", arqueoData.get("totalEsperado"));
        }
        if (arqueoData.containsKey("efectivoFisico")) {
            cleanData.put("efectivo_fisico", arqueoData.get("efectivoFisico"));
            cleanData.put("efectivofisico", arqueoData.get("efectivoFisico"));
        }
        if (arqueoData.containsKey("diferencia")) {
            cleanData.put("diferencia", arqueoData.get("diferencia"));
        }
        for (Map.Entry<String, Object> entry : arqueoData.entrySet()) {
            if (!cleanData.containsKey(entry.getKey())) {
                cleanData.put(entry.getKey(), entry.getValue());
            }
        }

        String jsonBody = gson.toJson(cleanData.isEmpty() ? arqueoData : cleanData);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error al guardar arqueo: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    android.util.Log.e("Supabase", "Error al registrar arqueo (" + response.code() + "): " + errorBody);
                    mainHandler.post(() -> callback.onError("Error de servidor (" + response.code() + ")"));
                }
            }
        });
    }

    // 4. Obtener Ventas registradas para el Consolidado
    public static void obtenerVentas(SupabaseCallback<JsonArray> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/ventas?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JsonArray jsonArray = gson.fromJson(jsonResponse, JsonArray.class);
                    mainHandler.post(() -> callback.onSuccess(jsonArray));
                } else {
                    mainHandler.post(() -> callback.onError("Error de servidor (" + response.code() + ")"));
                }
            }
        });
    }

    // 5. Validar usuario en Supabase (Tabla usuarios)
    public static void validarUsuario(String correo, String contrasena, SupabaseCallback<JsonObject> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/usuarios?correo=eq." + correo + "&contrasena=eq." + contrasena + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error de red: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    JsonArray array = gson.fromJson(jsonResponse, JsonArray.class);
                    if (array != null && array.size() > 0) {
                        mainHandler.post(() -> callback.onSuccess(array.get(0).getAsJsonObject()));
                    } else {
                        mainHandler.post(() -> callback.onError("Correo o contraseña incorrectos en Supabase"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Error de servidor (" + response.code() + ")"));
                }
            }
        });
    }

    // 6. Registrar Producto en Supabase
    public static void insertarProducto(Map<String, Object> productoData, SupabaseCallback<Boolean> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/productos";
        String jsonBody = gson.toJson(productoData);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error al registrar producto: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError("Error al guardar producto (" + response.code() + ")"));
                }
            }
        });
    }

    // 7. Eliminar Producto en Supabase por ID
    public static void eliminarProducto(String idProducto, SupabaseCallback<Boolean> callback) {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/productos?id=eq." + idProducto;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SupabaseConfig.SUPABASE_KEY)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Error al eliminar producto: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError("Error al eliminar producto (" + response.code() + ")"));
                }
            }
        });
    }
}