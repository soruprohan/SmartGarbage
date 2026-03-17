package com.example.smartgarbage.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {

    @SerializedName("routes")
    public List<Route> routes;

    @SerializedName("status")
    public String status;

    public static class Route {
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;

        @SerializedName("legs")
        public List<Leg> legs;

        // Google's optimized stop order — e.g. [2, 0, 1] means visit stop 2 first, then 0, then 1
        @SerializedName("waypoint_order")
        public List<Integer> waypointOrder;
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }

    public static class Leg {
        @SerializedName("duration")
        public TextValue duration;

        @SerializedName("distance")
        public TextValue distance;

        @SerializedName("start_address")
        public String startAddress;

        @SerializedName("end_address")
        public String endAddress;
    }

    public static class TextValue {
        @SerializedName("text")
        public String text;

        @SerializedName("value")
        public int value;
    }
}