package com.example.ricopan_ventas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CobroActivity extends AppCompatActivity {

    TextView tvTotalCobro;
    RadioGroup rgMetodoPago;
    View llProcesamientoEfectivo;
    EditText etMontoRecibido;
    Button btnConfirmarVenta;

    double totalPagar = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cobro);

        // 1. Vincular pantalla
        tvTotalCobro = findViewById(R.id.tvTotalCobro);
        rgMetodoPago = findViewById(R.id.rgMetodoPago);
        llProcesamientoEfectivo = findViewById(R.id.llProcesamientoEfectivo);
        etMontoRecibido = findViewById(R.id.etMontoRecibido);
        btnConfirmarVenta = findViewById(R.id.btnConfirmarVenta);

        // 2. Recuperar el total de la pantalla anterior
        totalPagar = getIntent().getDoubleExtra("TOTAL_VENTA", 0.0);
        tvTotalCobro.setText("S/. " + String.format(Locale.getDefault(), "%.2f", totalPagar));

        // 3. Lógica para ocultar el cuadro de efectivo si elige Yape/Plin
        rgMetodoPago.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbYape) {
                llProcesamientoEfectivo.setVisibility(View.GONE);
            } else {
                llProcesamientoEfectivo.setVisibility(View.VISIBLE);
            }
        });

        btnConfirmarVenta.setOnClickListener(v -> procesarVenta());
    }

    private void procesarVenta() {
        double montoRecibido = 0.0;
        double vuelto = 0.0;
        String metodoPago = "Efectivo";

        // Si es efectivo, verificamos cuánto entregó el cliente
        if (rgMetodoPago.getCheckedRadioButtonId() == R.id.rbEfectivo) {
            String montoStr = etMontoRecibido.getText().toString().trim().replace(",", ".");
            if (montoStr.isEmpty()) {
                Toast.makeText(this, "Ingresa el monto recibido", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                montoRecibido = Double.parseDouble(montoStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (montoRecibido < totalPagar) {
                Toast.makeText(this, "MONTO INSUFICIENTE", Toast.LENGTH_SHORT).show();
                return;
            }
            vuelto = montoRecibido - totalPagar;
        } else {
            // Si es Yape/Plin, el pago es exacto
            metodoPago = "Digital (Yape/Plin)";
            montoRecibido = totalPagar;
            vuelto = 0.0;
        }

        guardarEnFirebaseYMostrarTicket(metodoPago, montoRecibido, vuelto);
    }

    private void guardarEnFirebaseYMostrarTicket(String metodoPago, double montoRecibido, double vuelto) {
        String fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> venta = new HashMap<>();
        venta.put("fecha", fechaHora);
        venta.put("total", totalPagar);
        venta.put("montoRecibido", montoRecibido);
        venta.put("vuelto", vuelto);
        venta.put("metodo", metodoPago);

        SupabaseRepository.registrarVenta(venta, new SupabaseRepository.SupabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                String idTicket = "TICKET-" + System.currentTimeMillis();
                mostrarTicket(idTicket, fechaHora, metodoPago, montoRecibido, vuelto);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CobroActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarTicket(String idTicket, String fechaHora, String metodoPago, double montoRecibido, double vuelto) {
        // Inflar el diseño del ticket personalizado profesional
        View dialogView = LayoutInflater.from(this).inflate(R.layout.ticket_venta, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create();

        TextView tvDetalles = dialogView.findViewById(R.id.tvTicketDetalles);
        TextView tvProductos = dialogView.findViewById(R.id.tvTicketProductos);
        TextView tvTotalMonto = dialogView.findViewById(R.id.tvTicketTotalMonto);
        TextView tvMetodoPago = dialogView.findViewById(R.id.tvTicketMetodoPago);
        Button btnCompartirTicket = dialogView.findViewById(R.id.btnCompartirTicket);
        Button btnCerrarTicket = dialogView.findViewById(R.id.btnCerrarTicket);

        String numTicket = "#TKT-" + (idTicket != null && idTicket.length() >= 6 ? idTicket.substring(idTicket.length() - 5).toUpperCase() : "00002");

        // Llenar los datos del ticket idéntico al modelo profesional
        tvDetalles.setText(String.format(Locale.getDefault(),
                "Ticket N°: %s\nFecha y Hora: %s\nTurno: Turno 1: Mañana\nCajero: Vendedor / Cajero",
                numTicket, fechaHora));

        tvProductos.setText("• Venta Registrada en Caja (S/. " + String.format(Locale.getDefault(), "%.2f", totalPagar) + ")");
        tvTotalMonto.setText(String.format(Locale.getDefault(), "S/. %.2f", totalPagar));
        tvMetodoPago.setText("Medio de Pago: " + metodoPago.toUpperCase());

        btnCompartirTicket.setOnClickListener(v -> {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Comprobante Rico Pan - " + numTicket);
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "RICO PAN - Comprobante Digital\n" + numTicket + "\nTotal: S/. " + String.format(Locale.getDefault(), "%.2f", totalPagar) + "\nMétodo: " + metodoPago);
            startActivity(android.content.Intent.createChooser(shareIntent, "Compartir Ticket"));
        });

        btnCerrarTicket.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Cierra esta pantalla y regresa automáticamente al catálogo de Ventas
        });

        dialog.show();
    }
}