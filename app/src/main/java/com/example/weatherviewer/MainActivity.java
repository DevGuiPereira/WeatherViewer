package com.example.weatherviewer;

// Importações necessárias para UI, Rede e Processamento de Dados
import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity: Classe principal que gerencia a interface do usuário e a lógica do aplicativo.
 * Responsável por buscar a previsão do tempo e exibi-la em uma lista.
 */
public class MainActivity extends AppCompatActivity {

    // Lista que armazenará os objetos Weather (cada dia da previsão)
    private List<Weather> weatherList = new ArrayList<>();

    // Adaptador para conectar a lista de dados (weatherList) ao componente visual (ListView)
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView;

    // Campo de texto para entrada da cidade
    private EditText locationEditText;

    // Componentes da tela de erro (Layout, Mensagem e Ícone)
    private LinearLayout errorLayout;
    private TextView errorMessage;
    private ImageView errorIcon;

    // Configurações da API (Chave e URL Base)
    private final String API_KEY = BuildConfig.API_KEY;
    private final String BASE_URL = "http://agent-weathermap-env-env.eba-6pzgqekp.us-east-2.elasticbeanstalk.com/api/weather";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Define o layout da tela


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            window.setStatusBarColor(getResources().getColor(R.color.weather_blue_dark));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View decor = window.getDecorView();
                decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }

        weatherListView = findViewById(R.id.weatherListView);
        locationEditText = findViewById(R.id.locationEditText);
        errorLayout = findViewById(R.id.errorLayout);
        errorMessage = findViewById(R.id.errorMessage);
        errorIcon = findViewById(R.id.errorIcon);
        FloatingActionButton fab = findViewById(R.id.fab);

        // Inicializa o adaptador e conecta à ListView
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        // Configura o ouvinte de clique do botão de pesquisa (FAB)
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Obtém o texto digitado
                String city = locationEditText.getText().toString();

                if (!city.isEmpty()) {
                    // Cria a URL da requisição
                    URL url = createURL(city);

                    if (url != null) {
                        dismissKeyboard(locationEditText); // Esconde o teclado

                        // Reseta a interface: esconde erro e mostra lista
                        errorLayout.setVisibility(View.GONE);
                        weatherListView.setVisibility(View.VISIBLE);

                        // Inicia a tarefa assíncrona para baixar os dados
                        new GetWeatherTask().execute(url);
                    }
                } else {
                    // Feedback visual caso o campo esteja vazio
                    Toast.makeText(MainActivity.this, "Digite uma cidade", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Método utilitário para esconder o teclado virtual
    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Método que constrói a URL formatada para a API
    private URL createURL(String city) {
        try {
            // Codifica o nome da cidade (ex: espaços viram %20) para ser válido na URL
            String urlString = BASE_URL + "?city=" + URLEncoder.encode(city, "UTF-8")
                    + "&days=7" // Solicita 7 dias de previsão
                    + "&APPID=" + API_KEY; // Adiciona a chave de autenticação
            return new URL(urlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * GetWeatherTask: Classe interna para realizar operações de rede em uma Thread separada (Background).
     * Evita travar a interface do usuário durante o download.
     */
    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {
        private AlertDialog progressDialog; // Pop-up de "Carregando..."

        // Variáveis de controle de erro
        private boolean isConnectionError = false;
        private int lastResponseCode = -1;

        // Executado na Thread Principal ANTES do download começar
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isConnectionError = false;
            lastResponseCode = -1;

            // Configura e mostra o diálogo de progresso
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Buscando previsão...");
            builder.setCancelable(false); // Impede cancelamento pelo usuário

            // Adiciona uma barra de progresso circular (spinner)
            ProgressBar loadingSpinner = new ProgressBar(MainActivity.this);
            loadingSpinner.setPadding(20, 20, 20, 20);
            builder.setView(loadingSpinner);

            progressDialog = builder.create();
            progressDialog.show();
        }

        // Executado em uma Thread Secundária (Background)
        @Override
        protected JSONObject doInBackground(URL... params) {
            HttpURLConnection connection = null;
            try {
                // Abre a conexão HTTP
                connection = (HttpURLConnection) params[0].openConnection();

                // Obtém o código de resposta (Ex: 200 OK, 404 Not Found)
                lastResponseCode = connection.getResponseCode();
                Log.d("API_DEBUG", "Código HTTP: " + lastResponseCode);

                // Se a resposta for 200 (OK), lê os dados
                if (lastResponseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    }
                    // Retorna os dados como um Objeto JSON
                    return new JSONObject(builder.toString());
                }
            } catch (IOException e) {
                // Captura erros de rede (ex: sem internet)
                isConnectionError = true;
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Garante que a conexão seja fechada
                if (connection != null) connection.disconnect();
            }
            return null;
        }

        // Executado na Thread Principal DEPOIS do download terminar
        @Override
        protected void onPostExecute(JSONObject weather) {
            // Fecha o diálogo de progresso
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            // Limpa dados antigos da lista
            weatherList.clear();
            weatherArrayAdapter.notifyDataSetChanged();

            if (weather != null) {
                // CASO DE SUCESSO: Mostra a lista e processa o JSON
                weatherListView.setVisibility(View.VISIBLE);
                errorLayout.setVisibility(View.GONE);

                convertJSONtoArrayList(weather); // Converte JSON para Objetos Weather
                weatherArrayAdapter.notifyDataSetChanged(); // Atualiza a UI
                weatherListView.smoothScrollToPosition(0); // Volta ao topo da lista
                Toast.makeText(MainActivity.this, "Previsão atualizada!", Toast.LENGTH_SHORT).show();

            } else {
                // CASO DE FALHA: Mostra a tela de erro
                weatherListView.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);

                if (isConnectionError) {
                    // Erro 1: Falha na conexão (Internet)
                    errorIcon.setImageResource(R.drawable.ic_wifi_off);
                    errorMessage.setText("Sem conexão com a internet.\nVerifique seu Wi-Fi.");
                } else if (lastResponseCode == 404) {
                    // Erro 2: Cidade não encontrada
                    errorIcon.setImageResource(R.drawable.ic_error);
                    errorMessage.setText("Cidade não encontrada.\nVerifique a grafia.");
                } else if (lastResponseCode >= 500) {
                    // Erro 3: Erro interno do servidor
                    errorIcon.setImageResource(R.drawable.ic_error);
                    errorMessage.setText("Servidor indisponível.\nTente novamente mais tarde.");
                } else {
                    // Erro 4: Genérico
                    errorIcon.setImageResource(R.drawable.ic_error);
                    errorMessage.setText("Ocorreu um erro ao buscar os dados.");
                }
            }
        }
    }

    // Método para converter o JSON bruto em objetos da classe Weather
    private void convertJSONtoArrayList(JSONObject forecast) {
        try {
            // Navega na estrutura do JSON: Array "days"
            JSONArray list = forecast.getJSONArray("days");

            // Itera sobre cada dia da previsão
            for (int i = 0; i < list.length(); i++) {
                JSONObject day = list.getJSONObject(i);

                // Extrai os campos específicos
                String date = day.getString("date");
                double minTemp = day.getDouble("minTempC");
                double maxTemp = day.getDouble("maxTempC");
                String description = day.getString("description");
                double humidity = day.getDouble("humidity");
                String icon = day.getString("icon");

                // Cria o objeto Weather e adiciona à lista
                weatherList.add(new Weather(date, minTemp, maxTemp, humidity, description, icon));
            }
        } catch (JSONException e) {
            Log.e("API_DEBUG", "Erro ao ler JSON: " + e.getMessage());
        }
    }
}