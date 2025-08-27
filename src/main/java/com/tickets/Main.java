package com.tickets;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File("tickets.json");
            TicketsWrapper wrapper = mapper.readValue(file, TicketsWrapper.class);

            // Фильтр рейсов из Владивостока в Тель-Авив
            List<Ticket> tickets = ticketsFilter("VVO", "TLV",wrapper.getTickets());

            // Минимальное время полёта для каждого перевозчика
            System.out.println("Минимальное время полёта для каждого перевозчика:");
            Map<String, Long> minFlightTimes = calculateMinFlightTime(tickets);
            minFlightTimes.forEach((carrier, minutes) ->
                    System.out.printf("Перевозчик %s: %d мин (%d ч %d мин)%n",
                            carrier, minutes, minutes / 60, minutes % 60)
            );

            System.out.println();

            // Разница между средней ценой и медианой
            System.out.println("Статистика по цене:");
            double averagePrice = tickets.stream()
                    .mapToInt(Ticket::getPrice)
                    .average()
                    .orElse(0.0);

            double medianPrice = calculateMedian(tickets.stream()
                    .mapToInt(Ticket::getPrice)
                    .sorted()
                    .toArray());

            System.out.printf("Средняя цена: %.2f руб.%n", averagePrice);
            System.out.printf("Медианная цена: %.2f руб.%n", medianPrice);
            System.out.printf("Разница: %.2f руб.%n", averagePrice - medianPrice);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Фильтр билетов по отправке и назначению
    private static List<Ticket> ticketsFilter(String origin, String destination, List<Ticket> tickets) {
        return tickets.stream()
                .filter(t -> origin.equals(t.getOrigin()) && destination.equals(t.getDestination()))
                .collect(Collectors.toList());
    }

    // Минимальное время полёта для каждого перевозчика
    private static Map<String, Long> calculateMinFlightTime(List<Ticket> tickets) {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        Ticket::getCarrier,
                        Collectors.mapping(ticket -> {
                            LocalDateTime departure = LocalDateTime.of(
                                    parseLocalDate(ticket.getDeparture_date()),
                                    parseLocalTime(ticket.getDeparture_time())
                            );
                            LocalDateTime arrival = LocalDateTime.of(
                                    parseLocalDate(ticket.getArrival_date()),
                                    parseLocalTime(ticket.getArrival_time())
                            );
                            return Duration.between(departure, arrival).toMinutes();
                        }, Collectors.minBy(Long::compare))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(0L)
                ));
    }

    // Медианы
    private static double calculateMedian(int[] sortedPrices) {
        int n = sortedPrices.length;
        if (n == 0) return 0.0;
        if (n % 2 == 1) {
            return sortedPrices[n / 2];
        } else {
            return (sortedPrices[n / 2 - 1] + sortedPrices[n / 2]) / 2.0;
        }
    }

    private static LocalDate parseLocalDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMAT);
    }

    private static LocalTime parseLocalTime(String timeStr) {
        return LocalTime.parse(timeStr, TIME_FORMAT);
    }
}
