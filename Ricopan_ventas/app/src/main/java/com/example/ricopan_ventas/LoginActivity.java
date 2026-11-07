package com.example.ricopan_ventas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.JsonObject;

public class LoginActivity extends AppCompatActivity {

    EditText etCorreo, etContrasena;
    Button btnEntrar;
    RadioGroup rgRol;
    LinearLayout llTurnos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        btnEntrar = findViewById(R.id.btnIniciarSesion);
        rgRol = findViewById(R.id.rgRol);
        llTurnos = findViewById(R.id.rgTurno);

        rgRol.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbAdmin) {
                llTurnos.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbVendedor) {
                llTurnos.setVisibility(View.VISIBLE);
            }
        });

        btnEntrar.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String contrasena = etContrasena.getText().toString().trim();

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean esAdminSeleccionado = (rgRol.getCheckedRadioButtonId() == R.id.rbAdmin);

            if (esAdminSeleccionado && !correo.toLowerCase().contains("admin")) {
                Toast.makeText(LoginActivity.this, "El correo de Administrador debe incluir 'admin'", Toast.LENGTH_LONG).show();
                return;
            }

            // Validar con Supabase
            SupabaseRepository.validarUsuario(correo, contrasena, new SupabaseRepository.SupabaseCallback<JsonObject>() {
                @Override
                public void onSuccess(JsonObject result) {
                    Toast.makeText(LoginActivity.this, "¡INGRESO EXITOSO!", Toast.LENGTH_SHORT).show();
                    iniciarNavegacion(esAdminSeleccionado);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void iniciarNavegacion(boolean esAdminSeleccionado) {
        String rolSeleccionado = esAdminSeleccionado ? "Administrador" : "Vendedor";
        Intent intent = new Intent(LoginActivity.this, VentasActivity.class);
        intent.putExtra("TIPO_ROL", rolSeleccionado);
        startActivity(intent);
        finish();
    }
}