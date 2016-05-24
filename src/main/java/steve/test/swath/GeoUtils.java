package steve.test.swath;

public final class GeoUtils
{
    private static final double a = 6378137; // m (same for WGS84 and GRS80)

    private static final double EPS5 = 1e-5;
    private static final double EPS = 1e-10;

    public enum EarthModel { WGS84, GRS80 }

    private GeoUtils()
    {
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate (WGS84 geodetic system is used).
     * @param geoPos The geodetic coordinate of a given pixel.
     * @param xyz The xyz coordinates of the given pixel.
     * @throws MathUtilsException 
     */
    public static void geo2xyz(double[] geoPos, double xyz[]) throws MathUtilsException {
        geo2xyz(geoPos[0], geoPos[1], 0.0, xyz, EarthModel.WGS84);
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate with specified geodetic system.
     * @param geoPos The geodetic coordinate of a given pixel.
     * @param xyz The xyz coordinates of the given pixel.
     * @param geoSystem The geodetic system.
     * @throws MathUtilsException 
     */
    public static void geo2xyz(double[] geoPos, double xyz[], EarthModel geoSystem) throws MathUtilsException {
        geo2xyz(geoPos[0], geoPos[1], 0.0, xyz, geoSystem);
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate with specified geodetic system.
     * @param latitude The latitude of a given pixel (in degree).
     * @param longitude The longitude of the given pixel (in degree).
     * @param altitude The altitude of the given pixel (in m)
     * @param xyz The xyz coordinates of the given pixel.
     * @param geoSystem The geodetic system.
     * @throws MathUtilsException 
     */
    public static void geo2xyz(double latitude, double longitude, double altitude, double xyz[], EarthModel geoSystem) throws MathUtilsException {

        double a = 0.0;
        double earthFlatCoef = 0.0;

        if (geoSystem == EarthModel.WGS84) {

            a = WGS84.a;
            earthFlatCoef = WGS84.earthFlatCoef;

        } else if (geoSystem == EarthModel.GRS80) {

            a = GRS80.a;
            earthFlatCoef = GRS80.earthFlatCoef;

        } else {
            throw new MathUtilsException("Incorrect geodetic system");
        }

        final double e2 = 2.0 / earthFlatCoef - 1.0 / (earthFlatCoef * earthFlatCoef);

        final double lat = latitude * MathUtils.DTOR;
        final double lon = longitude * MathUtils.DTOR;

        final double sinLat = Math.sin(lat);
        final double cosLat = Math.cos(lat);
        final double N = a / Math.sqrt(1 - e2*sinLat*sinLat);

        xyz[0] = (N + altitude) * cosLat * Math.cos(lon); // in m
        xyz[1] = (N + altitude) * cosLat * Math.sin(lon); // in m
        xyz[2] = ((1 - e2) * N + altitude) * sinLat;   // in m
    }

    /**
     * Convert cartesian XYZ coordinate into geodetic coordinate (WGS84 geodetic system is used).
     * @param xyz The xyz coordinate of the given pixel.
     * @param geoPos The geodetic coordinate of the given pixel.
     * @throws MathUtilsException 
     */
    public static void xyz2geo(double xyz[], double[] geoPos) throws MathUtilsException {
        xyz2geo(xyz, geoPos, EarthModel.WGS84);
    }

    /**
     * Convert cartesian XYZ coordinate into geodetic coordinate with specified geodetic system.
     * @param xyz The xyz coordinate of the given pixel.
     * @param geoPos The geodetic coordinate of the given pixel.
     * @param geoSystem The geodetic system.
     * @throws MathUtilsException 
     */
    public static void xyz2geo(double xyz[], double[] geoPos, EarthModel geoSystem) throws MathUtilsException {

        double a = 0.0;
        double b = 0.0;
        double earthFlatCoef = 0.0;

        if (geoSystem == EarthModel.WGS84) {

            a = WGS84.a;
            b = WGS84.b;
            earthFlatCoef = WGS84.earthFlatCoef;

        } else if (geoSystem == EarthModel.GRS80) {

            a = GRS80.a;
            b = GRS80.b;
            earthFlatCoef = GRS80.earthFlatCoef;

        } else {
            throw new MathUtilsException("Incorrect geodetic system");
        }

        final double e2 = 2.0 / earthFlatCoef - 1.0 / (earthFlatCoef * earthFlatCoef);
        final double ep2 = e2 / (1 - e2);

        final double x = xyz[0];
        final double y = xyz[1];
        final double z = xyz[2];
        final double s = Math.sqrt(x*x + y*y);
        final double theta = Math.atan(z*a/(s*b));

        geoPos[1] = (float)(Math.atan(y/x) * MathUtils.RTOD);
        
        if (geoPos[1] < 0.0 && y >= 0.0) {
            geoPos[1] += 180.0;
        } else if (geoPos[1] > 0.0 && y < 0.0) {
            geoPos[1] -= 180.0;
        }

        geoPos[0] = (float)(Math.atan((z + ep2*b*Math.pow(Math.sin(theta), 3)) /
                                       (s - e2*a*Math.pow(Math.cos(theta), 3))) *
                                       MathUtils.RTOD);
    }


    /**
     // Given starting point GLON1,GLAT1, head1 = initial heading,and distance
     // in meters, calculate destination GLON2,GLAT2, and head2=initial heading
     // from destination to starting point

     // Input:
     // lon1:	longitude
     // lat1:	latitude
     // dist:	distance in m
     // head1:	azimuth in degree measured in the diretion North east south west

     // Output:
     // GLON2:	longitude
     // GLAT2:	latitude
     // head2:	azimuth in degree measured in the direction North east south west
     //			from (GLON2,GLAT2) to (GLON1, GLAT1)
     */
    public static LatLonHeading vincenty_direct(double lon1, double lat1, double dist, double head1) {

        final LatLonHeading pos = new LatLonHeading();

        lat1 *= MathUtils.DTOR;
        lon1 *= MathUtils.DTOR;
        final double  FAZ = head1 * MathUtils.DTOR;

        // Model WGS84:
        //    F=1/298.25722210;	// flatteing
        final double F = 0.0;  // defF

        // equatorial radius
        final double R = 1.0 - F;
        double TU = R * Math.tan(lat1);
        final double SF = Math.sin(FAZ);
        final double CF = Math.cos(FAZ);
        double BAZ = 0.0;
        if (CF != 0.0)
            BAZ = Math.atan2(TU, CF) * 2.0;
        final double CU = 1.0 / Math.sqrt(TU * TU + 1.0);
        final double SU = TU * CU;
        final double SA = CU * SF;
        final double C2A = -SA * SA + 1.0;
        double X = Math.sqrt((1.0 / R / R - 1.0) * C2A + 1.0) + 1.0;
        X = (X - 2.0) / X;
        double C = 1.0 - X;
        C = (X * X / 4.0 + 1) / C;
        double D = (0.375 * X * X - 1.0) * X;
        TU = dist / R / a / C;
        double Y = TU;

        double SY, CY, CZ, E;
        do {
            SY = Math.sin(Y);
            CY = Math.cos(Y);
            CZ = Math.cos(BAZ + Y);
            E = CZ * CZ * 2.0 - 1.0;
            C = Y;
            X = E * CY;
            Y = E + E - 1.0;
            Y = (((SY * SY * 4.0 - 3.0) * Y * CZ * D / 6.0 + X) * D / 4.0 - CZ) * SY * D + TU;
        } while (Math.abs(Y - C) > EPS);

        BAZ = CU * CY * CF - SU * SY;
        C = R * Math.sqrt(SA * SA + BAZ * BAZ);
        D = SU * CY + CU * SY * CF;
        pos.lat = Math.atan2(D, C);
        C = CU * CY - SU * SY * CF;
        X = Math.atan2(SY * SF, C);
        C = ((-3.0 * C2A + 4.0) * F + 4.0) * C2A * F / 16.0;
        D = ((E * CY * C + CZ) * SY * C + Y) * SA;
        pos.lon = lon1 + X - (1.0 - C) * D * F;
        BAZ = Math.atan2(SA, BAZ) + Math.PI;

        pos.lon *= MathUtils.RTOD;
        pos.lat *= MathUtils.RTOD;
        pos.heading = BAZ * MathUtils.RTOD;

        while (pos.heading < 0)
           pos.heading += 360;

        return pos;
    }

    /**
     * // Given starting (GLON1,GLAT1) and end points (GLON2,GLAT2)
     * // calculate distance in meters and initial headings from start to
     * // end (return variable head1),
     * // and from end to start point (return variable head2)
     * <p/>
     * // Input:
     * // lon1:	longitude
     * // lat1:	latitude
     * // lon2:	longitude
     * // lat2:	latitude
     * <p/>
     * // Output:
     * // dist:	distance in m
     * // head1:	azimuth in degrees mesured in the direction North east south west
     * //			from (lon1,lat1) to (lon2, lat2)
     * // head2:	azimuth in degrees mesured in the direction North east south west
     * //			from (lon2,lat2) to (lon1, lat1)
     */
    public static DistanceHeading vincenty_inverse(double lon1, double lat1, double lon2, double lat2) {

        final DistanceHeading output = new DistanceHeading();

        if ((Math.abs(lon1 - lon2) < EPS5) && (Math.abs(lat1 - lat2) < EPS5)) {
            output.distance = 0;
            output.heading1 = -1;
            output.heading2 = -1;
            return output;
        }

        lat1 *= MathUtils.DTOR;
        lat2 *= MathUtils.DTOR;
        lon1 *= MathUtils.DTOR;
        lon2 *= MathUtils.DTOR;

        // Model WGS84:
        //    F=1/298.25722210;	// flattening
        final double F = 0.0; //defF;

        final double R = 1 - F;
        double TU1 = R * Math.tan(lat1);
        double TU2 = R * Math.tan(lat2);
        final double CU1 = 1.0 / Math.sqrt(TU1 * TU1 + 1.0);
        final double SU1 = CU1 * TU1;
        final double CU2 = 1.0 / Math.sqrt(TU2 * TU2 + 1.0);
        double S = CU1 * CU2;
        double BAZ = S * TU2;
        double FAZ = BAZ * TU1;
        double X = lon2 - lon1;

        double SX, CX, SY, CY, Y, SA, C2A, CZ, E, C, D;
        do {
            SX = Math.sin(X);
            CX = Math.cos(X);
            TU1 = CU2 * SX;
            TU2 = BAZ - SU1 * CU2 * CX;
            SY = Math.sqrt(TU1 * TU1 + TU2 * TU2);
            CY = S * CX + FAZ;
            Y = Math.atan2(SY, CY);
            SA = S * SX / SY;
            C2A = -SA * SA + 1.;
            CZ = FAZ + FAZ;
            if (C2A > 0.)
                CZ = -CZ / C2A + CY;
            E = CZ * CZ * 2. - 1.;
            C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
            D = X;
            X = ((E * CY * C + CZ) * SY * C + Y) * SA;
            X = (1. - C) * X * F + lon2 - lon1;
        } while (Math.abs(D - X) > (0.01));

        FAZ = Math.atan2(TU1, TU2);
        BAZ = Math.atan2(CU1 * SX, BAZ * CX - SU1 * CU2) + Math.PI;
        X = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
        X = (X - 2.) / X;
        C = 1. - X;
        C = (X * X / 4. + 1.) / C;
        D = (0.375 * X * X - 1.) * X;
        X = E * CY;
        S = 1. - E - E;
        S = ((((SY * SY * 4. - 3.) * S * CZ * D / 6. - X) * D / 4. + CZ) * SY * D + Y) * C * a * R;

        output.distance = S;
        output.heading1 = FAZ * MathUtils.RTOD;
        output.heading2 = BAZ * MathUtils.RTOD;
        
        while (output.heading1< 0)
            output.heading1 += 360;
        while (output.heading2<0)
                        output.heading2+=360;

        return output;
    }

    public static class LatLonHeading {
        public double lat;
        public double lon;
        public double heading;
    }

    public static class DistanceHeading {
        public double distance;
        public double heading1;
        public double heading2;
    }

    public static interface WGS84 {
        public static final double a = 6378137; // m
        public static final double b = 6356752.314245; // m
        public static final double earthFlatCoef = 298.257223563;
    }

    public static interface GRS80 {
        public static final double a = 6378137; // m
        public static final double b = 6356752.314140 ; // m
        public static final double earthFlatCoef = 298.257222101;
    }
}