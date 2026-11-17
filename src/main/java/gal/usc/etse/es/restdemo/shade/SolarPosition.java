package gal.usc.etse.es.restdemo.shade;

import java.time.Instant;

/**
 * Posición do sol para un instante e un punto da Terra, segundo o algoritmo simplificado da NOAA
 * (precisión de ~0.5° entre 1901 e 2099, máis que suficiente para decidir se un banco ten sol).
 *
 * @param elevation graos sobre o horizonte (negativo = de noite)
 * @param azimuth   graos dende o norte no sentido das agullas do reloxo
 */
public record SolarPosition(double elevation, double azimuth) {

    public static SolarPosition at(Instant instant, double latitude, double longitude) {
        double julianDay = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5;
        double t = (julianDay - 2_451_545.0) / 36_525.0; // séculos xulianos dende J2000

        double meanLongitude = mod(280.46646 + t * (36000.76983 + t * 0.0003032), 360);
        double meanAnomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);

        double m = Math.toRadians(meanAnomaly);
        double center = Math.sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(2 * m) * (0.019993 - 0.000101 * t)
                + Math.sin(3 * m) * 0.000289;

        double omega = Math.toRadians(125.04 - 1934.136 * t);
        double apparentLongitude = Math.toRadians(meanLongitude + center - 0.00569 - 0.00478 * Math.sin(omega));

        double meanObliquity = 23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60;
        double obliquity = Math.toRadians(meanObliquity + 0.00256 * Math.cos(omega));

        double declination = Math.asin(Math.sin(obliquity) * Math.sin(apparentLongitude));

        // Ecuación do tempo (minutos)
        double y = Math.pow(Math.tan(obliquity / 2), 2);
        double l0 = Math.toRadians(meanLongitude);
        double equationOfTime = 4 * Math.toDegrees(
                y * Math.sin(2 * l0)
                        - 2 * eccentricity * Math.sin(m)
                        + 4 * eccentricity * y * Math.sin(m) * Math.cos(2 * l0)
                        - 0.5 * y * y * Math.sin(4 * l0)
                        - 1.25 * eccentricity * eccentricity * Math.sin(2 * m));

        double minutesUtc = mod(instant.getEpochSecond() / 60.0, 1440);
        double trueSolarTime = mod(minutesUtc + equationOfTime + 4 * longitude, 1440);
        double hourAngle = Math.toRadians(trueSolarTime / 4 < 0 ? trueSolarTime / 4 + 180 : trueSolarTime / 4 - 180);

        double lat = Math.toRadians(latitude);
        double cosZenith = Math.sin(lat) * Math.sin(declination) + Math.cos(lat) * Math.cos(declination) * Math.cos(hourAngle);
        double zenith = Math.acos(Math.max(-1, Math.min(1, cosZenith)));

        double azimuth = Math.toDegrees(Math.atan2(
                Math.sin(hourAngle),
                Math.cos(hourAngle) * Math.sin(lat) - Math.tan(declination) * Math.cos(lat))) + 180;

        return new SolarPosition(90 - Math.toDegrees(zenith), mod(azimuth, 360));
    }

    private static double mod(double value, double modulus) {
        double r = value % modulus;
        return r < 0 ? r + modulus : r;
    }
}
