package com.example.ricopan_ventas;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportesCajaActivity extends AppCompatActivity {

    Button btnTabHistorial, btnTabConsolidado;
    Button btnFiltroTodosTurnos, btnFiltroTurno1, btnFiltroTurno2;
    ScrollView layoutHistorial;
    View layoutConsolidado;
    LinearLayout llListaHistorial;

    TextView tvConsolidadoTotal, tvConsolidadoCantidadVentas, tvConsolidadoEfectivo, tvConsolidadoDigital;
    TextView tvTurno1Monto, tvTurno1Detalle, tvTurno2Monto, tvTurno2Detalle;

    List<JsonObject> listaVentasGlobal = new ArrayList<>();
    String filtroTurnoActual = "TODOS"; // TODOS, TURNO1, TURNO2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes_caja);

        vincularVistas();
        configurarTabs();
        configurarFiltrosTurnos();
        cargarVentasDesdeSupabase();
    }

    private void vincularVistas() {
        btnTabHistorial = findViewById(R.id.btnTabHistorial);
        btnTabConsolidado = findViewById(R.id.btnTabConsolidado);
        btnFiltroTodosTurnos = findViewById(R.id.btnFiltroTodosTurnos);
        btnFiltroTurno1 = findViewById(R.id.btnFiltroTurno1);
        btnFiltroTurno2 = findViewById(R.id.btnFiltroTurno2);
        layoutHistorial = findViewById(R.id.layoutHistorial);
        layoutConsolidado = findViewById(R.id.layoutConsolidado);
        llListaHistorial = findViewById(R.id.llListaHistorial);

        tvConsolidadoTotal = findViewById(R.id.tvConsolidadoTotal);
        tvConsolidadoCantidadVentas = findViewById(R.id.tvConsolidadoCantidadVentas);
        tvConsolidadoEfectivo = findViewById(R.id.tvConsolidadoEfectivo);
        tvConsolidadoDigital = findViewById(R.id.tvConsolidadoDigital);
        tvTurno1Monto = findViewById(R.id.tvTurno1Monto);
        tvTurno1Detalle = findViewById(R.id.tvTurno1Detalle);
        tvTurno2Monto = findViewById(R.id.tvTurno2Monto);
        tvTurno2Detalle = findViewById(R.id.tvTurno2Detalle);
    }

    private void configurarTabs() {
        btnTabHistorial.setOnClickListener(v -> {
            btnTabHistorial.setBackgroundColor(Color.parseColor("#8B4513"));
            btnTabHistorial.setTextColor(Color.WHITE);
            btnTabConsolidado.setBackgroundColor(Color.parseColor("#65350F"));
            btnTabConsolidado.setTextColor(Color.parseColor("#D7CCC8"));

            layoutHistorial.setVisibility(View.VISIBLE);
            layoutConsolidado.setVisibility(View.GONE);
        });

        btnTabConsolidado.setOnClickListener(v -> {
            btnTabConsolidado.setBackgroundColor(Color.parseColor("#8B4513"));
            btnTabConsolidado.setTextColor(Color.WHITE);
            btnTabHistorial.setBackgroundColor(Color.parseColor("#65350F"));
            btnTabHistorial.setTextColor(Color.parseColor("#D7CCC8"));

            layoutConsolidado.setVisibility(View.VISIBLE);
            layoutHistorial.setVisibility(View.GONE);
            calcularConsolidado(listaVentasGlobal);
        });
    }

    private void configurarFiltrosTurnos() {
        View.OnClickListener filtroListener = v -> {
            btnFiltroTodosTurnos.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btnFiltroTodosTurnos.setTextColor(Color.parseColor("#333333"));
            btnFiltroTurno1.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btnFiltroTurno1.setTextColor(Color.parseColor("#333333"));
            btnFiltroTurno2.setBackgroundColor(Color.parseColor("#FFFFFF"));
            btnFiltroTurno2.setTextColor(Color.parseColor("#333333"));

            Button b = (Button) v;
            b.setBackgroundColor(Color.parseColor("#8B4513"));
            b.setTextColor(Color.WHITE);

            int id = v.getId();
            if (id == R.id.btnFiltroTodosTurnos) {
                filtroTurnoActual = "TODOS";
            } else if (id == R.id.btnFiltroTurno1) {
                filtroTurnoActual = "TURNO1";
            } else if (id == R.id.btnFiltroTurno2) {
                filtroTurnoActual = "TURNO2";
            }

            filtrarYMostrarVentas(listaVentasGlobal);
        };

        btnFiltroTodosTurnos.setOnClickListener(filtroListener);
        btnFiltroTurno1.setOnClickListener(filtroListener);
        btnFiltroTurno2.setOnClickListener(filtroListener);
    }

    private void cargarVentasDesdeSupabase() {
        SupabaseRepository.obtenerVentas(new SupabaseRepository.SupabaseCallback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray result) {
                listaVentasGlobal.clear();
                if (result != null) {
                    for (int i = 0; i < result.size(); i++) {
                        listaVentasGlobal.add(result.get(i).getAsJsonObject());
                    }
                }
                filtrarYMostrarVentas(listaVentasGlobal);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ReportesCajaActivity.this, "Error al cargar ventas: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarYMostrarVentas(List<JsonObject> ventas) {
        List<JsonObject> filtradas = new ArrayList<>();
        for (JsonObject v : ventas) {
            String hora = v.has("fecha") && !v.get("fecha").isJsonNull() ? v.get("fecha").getAsString() : "";
            boolean esTurno1 = esHoraTurnoMañana(hora);

            if (filtroTurnoActual.equals("TODOS")) {
                filtradas.add(v);
            } else if (filtroTurnoActual.equals("TURNO1") && esTurno1) {
                filtradas.add(v);
            } else if (filtroTurnoActual.equals("TURNO2") && !esTurno1) {
                filtradas.add(v);
            }
        }
        mostrarHistorialEnPantalla(filtradas);
        calcularConsolidado(filtradas);
    }

    private boolean esHoraTurnoMañana(String fechaStr) {
        // Turno 1: Mañana (06:00 a.m. - 02:30 p.m.)
        if (fechaStr.contains(" 06:") || fechaStr.contains(" 07:") || fechaStr.contains(" 08:") ||
                fechaStr.contains(" 09:") || fechaStr.contains(" 10:") || fechaStr.contains(" 11:") ||
                fechaStr.contains(" 12:") || fechaStr.contains(" 13:") || fechaStr.contains(" 14:00")) {
            return true;
        }
        return false;
    }

    private void mostrarHistorialEnPantalla(List<JsonObject> ventas) {
        llListaHistorial.removeAllViews();
        if (ventas.isEmpty()) {
            TextView tvVacio = new TextView(this);
            tvVacio.setText("No hay ventas registradas en este turno.");
            tvVacio.setPadding(20, 40, 20, 20);
            tvVacio.setTextColor(Color.parseColor("#777777"));
            llListaHistorial.addView(tvVacio);
            return;
        }

        int index = ventas.size();
        for (JsonObject venta : ventas) {
            String idTkt = "#TKT-" + String.format(Locale.getDefault(), "%05d", index--);
            String fecha = venta.has("fecha") && !venta.get("fecha").isJsonNull() ? venta.get("fecha").getAsString() : "Fecha no disponible";
            double total = venta.has("total") && !venta.get("total").isJsonNull() ? venta.get("total").getAsDouble() : 0.0;
            String metodo = venta.has("metodo") && !venta.get("metodo").isJsonNull() ? venta.get("metodo").getAsString() : "Efectivo";
            boolean esTurno1 = esHoraTurnoMañana(fecha);
            String turnoStr = esTurno1 ? "Turno 1: Mañana" : "Turno 2: Tarde / Noche";

            CardView card = new CardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 14);
            card.setLayoutParams(params);
            card.setRadius(16);
            card.setCardElevation(3);
            card.setCardBackgroundColor(Color.WHITE);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(20, 16, 20, 16);

            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView tvTicket = new TextView(this);
            tvTicket.setText(idTkt);
            tvTicket.setTextSize(14);
            tvTicket.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTicket.setTextColor(Color.parseColor("#8B4513"));
            tvTicket.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvMonto = new TextView(this);
            tvMonto.setText(String.format(Locale.getDefault(), "S/. %.2f", total));
            tvMonto.setTextSize(16);
            tvMonto.setTypeface(null, android.graphics.Typeface.BOLD);
            tvMonto.setTextColor(Color.parseColor("#8B4513"));
            topRow.addView(tvTicket);
            topRow.addView(tvMonto);
            content.addView(topRow);

            TextView tvFecha = new TextView(this);
            tvFecha.setText(fecha);
            tvFecha.setTextSize(12);
            tvFecha.setTextColor(Color.parseColor("#666666"));
            tvFecha.setPadding(0, 4, 0, 4);
            content.addView(tvFecha);

            LinearLayout bottomRow = new LinearLayout(this);
            bottomRow.setOrientation(LinearLayout.HORIZONTAL);
            bottomRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            bottomRow.setPadding(0, 6, 0, 0);

            TextView tvCajero = new TextView(this);
            tvCajero.setText("Cajero: Vendedor | " + turnoStr);
            tvCajero.setTextSize(11);
            tvCajero.setTextColor(Color.parseColor("#555555"));
            tvCajero.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvMetodo = new TextView(this);
            tvMetodo.setText(metodo.toUpperCase());
            tvMetodo.setTextSize(11);
            tvMetodo.setTypeface(null, android.graphics.Typeface.BOLD);
            tvMetodo.setTextColor(metodo.toLowerCase().contains("efectivo") ? Color.parseColor("#2E7D32") : Color.parseColor("#7B1FA2"));

            bottomRow.addView(tvCajero);
            bottomRow.addView(tvMetodo);
            content.addView(bottomRow);

            card.addView(content);
            llListaHistorial.addView(card);
        }
    }

    private void calcularConsolidado(List<JsonObject> ventas) {
        double totalRecaudado = 0.0;
        double totalEfectivo = 0.0;
        double totalDigital = 0.0;

        double t1Monto = 0.0;
        int t1Count = 0;
        double t2Monto = 0.0;
        int t2Count = 0;

        for (JsonObject venta : ventas) {
            double total = venta.has("total") && !venta.get("total").isJsonNull() ? venta.get("total").getAsDouble() : 0.0;
            String metodo = venta.has("metodo") && !venta.get("metodo").isJsonNull() ? venta.get("metodo").getAsString() : "";
            String fecha = venta.has("fecha") && !venta.get("fecha").isJsonNull() ? venta.get("fecha").getAsString() : "";

            totalRecaudado += total;
            if (metodo.toLowerCase().contains("efectivo")) {
                totalEfectivo += total;
            } else {
                totalDigital += total;
            }

            if (esHoraTurnoMañana(fecha)) {
                t1Monto += total;
                t1Count++;
            } else {
                t2Monto += total;
                t2Count++;
            }
        }

        tvConsolidadoTotal.setText(String.format(Locale.getDefault(), "S/. %.2f", totalRecaudado));
        tvConsolidadoCantidadVentas.setText(ventas.size() + " Ventas Realizadas");
        tvConsolidadoEfectivo.setText(String.format(Locale.getDefault(), "S/. %.2f", totalEfectivo));
        tvConsolidadoDigital.setText(String.format(Locale.getDefault(), "S/. %.2f", totalDigital));

        tvTurno1Monto.setText(String.format(Locale.getDefault(), "S/. %.2f", t1Monto));
        tvTurno1Detalle.setText(t1Count + " transacciones registradas");
        tvTurno2Monto.setText(String.format(Locale.getDefault(), "S/. %.2f", t2Monto));
        tvTurno2Detalle.setText(t2Count + " transacciones registradas");
    }
}