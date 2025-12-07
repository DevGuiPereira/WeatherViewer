package com.example.weatherviewer; // Verifique se o pacote é o mesmo do seu projeto

import java.text.NumberFormat;
import java.util.Locale;

public class Weather {
    public final String dayOfWeek;
    public final String minTemp;
    public final String maxTemp;
    public final String humidity;
    public final String description;
    public final String icon; // Agora é uma String (emoji), não uma URL

    // Construtor atualizado para o JSON da atividade
    public Weather(String dateString, double minTemp, double maxTemp, double humidity, String description, String icon) {
        // A API retorna a data pronta (ex: "2025-11-26"), vamos usá-la direto ou formatar se preferir
        this.dayOfWeek = dateString;

        // Formatadores numéricos
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.getDefault());
        numberFormat.setMaximumFractionDigits(0);
        NumberFormat percentFormat = NumberFormat.getPercentInstance(Locale.getDefault());

        // Configuração das Strings para exibição (Adicionando °C conforme instrução )
        this.minTemp = numberFormat.format(minTemp) + "\u00B0C";
        this.maxTemp = numberFormat.format(maxTemp) + "\u00B0C";
        this.humidity = NumberFormat.getPercentInstance().format(humidity); // API retorna 0.75 para 75%
        this.description = description;
        this.icon = icon; // Emoji retornado diretamente pelo JSON
    }
}