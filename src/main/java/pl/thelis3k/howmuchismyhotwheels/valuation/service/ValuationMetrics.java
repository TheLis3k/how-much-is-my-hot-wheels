package pl.thelis3k.howmuchismyhotwheels.valuation.service;

public record ValuationMetrics(
        Double min,
        Double max,
        String maxLink,
        Double average,
        Double smartAverage,
        Integer count
) {
    public record Offer(Double price, String url) implements Comparable<Offer> {
        @Override
        public int compareTo(Offer o) {
            return Double.compare(this.price, o.price);
        }
    }
}