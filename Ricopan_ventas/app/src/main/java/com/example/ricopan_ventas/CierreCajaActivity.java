package com.example.ricopan_ventas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CierreCajaActivity extends AppCompatActivity {

    // Variables Globales
    TextView tvVentasManana, tvVentasTarde, tvTotalEsperado;
    EditText etEfectivoFisico;
    Button btnConfirmarCierre, btnExportarPDF;

    double sumaManana = 0.0;
    double sumaTarde = 0.0;
    double totalGeneral = 0.0;

    // Índice Principal
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cierre_caja);

        vincularVistas();
        cargarVentasEnSupabase();
        configurarBotones();
    }

    // Métodos Separados
    private void vincularVistas() {
        tvVentasManana = findViewById(R.id.tvVentasManana);
        tvVentasTarde = findViewById(R.id.tvVentasTarde);
        tvTotalEsperado = findViewById(R.id.tvTotalEsperado);
        etEfectivoFisico = findViewById(R.id.etEfectivoFisico);
        btnConfirmarCierre = findViewById(R.id.btnConfirmarCierre);
        btnExportarPDF = findViewById(R.id.btnExportarPDF);
    }

    private void cargarVentasEnSupabase() {
        SupabaseRepository.obtenerVentas(new SupabaseRepository.SupabaseCallback<JsonArray>() {
            @Override
            public void onSuccess(JsonArray result) {
                sumaManana = 0.0;
                sumaTarde = 0.0;

                if (result != null) {
                    for (int i = 0; i < result.size(); i++) {
                        JsonObject venta = result.get(i).getAsJsonObject();
                        double totalTicket = 0.0;
                        if (venta.has("total") && !venta.get("total").isJsonNull()) {
                            totalTicket = venta.get("total").getAsDouble();
                        }
                        String fecha = venta.has("fecha") && !venta.get("fecha").isJsonNull() ? venta.get("fecha").getAsString() : "";

                        if (fecha != null && (fecha.contains(" 06:") || fecha.contains(" 07:") || fecha.contains(" 08:") || fecha.contains(" 09:") || fecha.contains(" 10:") || fecha.contains(" 11:") || fecha.contains(" 12:") || fecha.contains(" 13:"))) {
                            sumaManana += totalTicket;
                        } else {
                            sumaTarde += totalTicket;
                        }
                    }
                }

                totalGeneral = sumaManana + sumaTarde;

                tvVentasManana.setText(String.format(Locale.getDefault(), "Turno Mañana: S/. %.2f", sumaManana));
                tvVentasTarde.setText(String.format(Locale.getDefault(), "Turno Tarde/Noche: S/. %.2f", sumaTarde));
                tvTotalEsperado.setText(String.format(Locale.getDefault(), "TOTAL GENERAL: S/. %.2f", totalGeneral));
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(CierreCajaActivity.this, "Error al cargar ventas de Supabase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarBotones() {
        // Botón de Cierre de Caja
        btnConfirmarCierre.setOnClickListener(v -> {
            String fisicoStr = etEfectivoFisico.getText().toString().trim().replace(",", ".");

            if (fisicoStr.isEmpty()) {
                Toast.makeText(this, "Ingrese el dinero físico contado en la gaveta", Toast.LENGTH_SHORT).show();
                return;
            }

            double efectivoFisico;
            try {
                efectivoFisico = Double.parseDouble(fisicoStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Ingrese un monto válido", Toast.LENGTH_SHORT).show();
                return;
            }

            double diferencia = efectivoFisico - totalGeneral;

            Map<String, Object> reporte = new HashMap<>();
            reporte.put("totalEsperado", totalGeneral);
            reporte.put("efectivoFisico", efectivoFisico);
            reporte.put("diferencia", diferencia);

            SupabaseRepository.registrarArqueo(reporte, new SupabaseRepository.SupabaseCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    String mensaje = diferencia == 0 ? "Caja Cuadrada Exacta." : "Diferencia de S/. " + diferencia;
                    Toast.makeText(CierreCajaActivity.this, "Cierre Exitoso. " + mensaje, Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CierreCajaActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Botón para exportar el PDF
        btnExportarPDF.setOnClickListener(v -> generarReportePDF());
    }

    private void generarReportePDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 400, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("RICO PAN - REPORTE DE VENTAS", 20, 40, paint);

        paint.setTextSize(14);
        paint.setFakeBoldText(false);
        canvas.drawText("Turno Mañana: S/. " + String.format(Locale.getDefault(), "%.2f", sumaManana), 20, 80, paint);
        canvas.drawText("Turno Tarde/Noche: S/. " + String.format(Locale.getDefault(), "%.2f", sumaTarde), 20, 110, paint);

        paint.setFakeBoldText(true);
        canvas.drawText("TOTAL ESPERADO: S/. " + String.format(Locale.getDefault(), "%.2f", totalGeneral), 20, 150, paint);

        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        canvas.drawText("Documento generado automáticamente.", 20, 350, paint);

        document.finishPage(page);

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reporte_RicoPan.pdf");

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Guardado exitosamente en tus Documentos", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al crear el PDF", Toast.LENGTH_SHORT).show();
        }

        document.close();
    }
}