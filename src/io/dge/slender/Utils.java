package io.dge.slender;

import Jama.Matrix;
import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void ltoast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static void stoast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public Triplet<Double, Double, Double> filterGyroscopeData(
            float x, float y, float z, long interval) {
        final double dt = (double) interval / 1000;

        double[][] inital_estimate_arr = { {0,0,0,0,0,0} };
        double[][] error_estimate_arr =  {
                {0.001, 0, 0, 0, 0, 0},
                {0, 0.001, 0, 0, 0, 0},
                {0, 0, 0.001, 0, 0, 0},
                {0, 0, 0, 0.001, 0, 0},
                {0, 0, 0, 0, 0.001, 0},
                {0, 0, 0, 0, 0, 0.001}
        };
        double[][] state_transition_arr = {
                {1, 0, 0, dt, 0, 0},
                {0, 1, 0, 0, dt, 0},
                {0, 0, 1, 0, 0, dt},
                {0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        };
        double[][] process_error_arr = {
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}
        };
        double[][] identity_arr = {
                { 1, 0, 0, 0, 0, 0},
                { 0, 1, 0, 0, 0, 0},
                { 0, 0, 1, 0, 0, 0},
                { 0, 0, 0, 1, 0, 0},
                { 0, 0, 0, 0, 1, 0},
                { 0, 0, 0, 0, 0, 1} };


        double measurement_error_arr[][] = {
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}
        };

        double[] xyz = { x, y, z };

        Matrix state_estimate = new Matrix(inital_estimate_arr);
        Matrix error_estimate = new Matrix(error_estimate_arr);
        Matrix innovation = new Matrix(6, 1);
        Matrix innovation_covar = new Matrix(6, 6);
        Matrix kalman_gain = new Matrix(6, 6);
        Matrix state_transition = new Matrix(state_transition_arr);
        //    Matrix observation = new Matrix(observation_arr);
        Matrix measurement_error = new Matrix(measurement_error_arr);
        Matrix process_error = new Matrix(process_error_arr);
        Matrix newest_measurement = new Matrix(6,1);
        Matrix identity = new Matrix(identity_arr);
        //Actual Algorithm

        //State Prediction
        state_estimate = state_transition.times(state_estimate);
        error_estimate = state_transition.times(state_estimate.times(state_transition.transpose())).plus(process_error);
        innovation = newest_measurement.minus(state_estimate);
        innovation_covar = error_estimate.plus(measurement_error);
        kalman_gain = error_estimate.times(innovation_covar.inverse());
        state_estimate = state_estimate.plus(kalman_gain.times(innovation));
        error_estimate = identity.minus(kalman_gain).times(error_estimate);

        return null;
    }

    public static class Triplet<A, B, C> {
        public A first;
        public B second;
        public C third;

        public Triplet(A a, B b, C c) {
            first = a;
            second = b;
            third = c;
        }
    }
}
