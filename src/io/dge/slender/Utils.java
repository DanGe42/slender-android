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

    public static class Triplet<A, B, C> {
        public A first;
        public B second;
        public C third;

        public Triplet(A a, B b, C c) {
            first = a;
            second = b;
            third = c;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            builder.append(first.toString());
            builder.append(", ");
            builder.append(second.toString());
            builder.append(", ");
            builder.append(third.toString());
            builder.append(')');

            return builder.toString();
        }
    }

    public static class GyroscopeFilter {
        private Matrix state_estimate, error_estimate, innovation, innovation_covar,
                kalman_gain;

        public GyroscopeFilter() {
            double[][] inital_estimate_arr = {{0},{0},{0},{0},{0},{0}};
            double[][] error_estimate_arr =  {
                    {0.001, 0, 0, 0, 0, 0},
                    {0, 0.001, 0, 0, 0, 0},
                    {0, 0, 0.001, 0, 0, 0},
                    {0, 0, 0, 0.001, 0, 0},
                    {0, 0, 0, 0, 0.001, 0},
                    {0, 0, 0, 0, 0, 0.001}
            };

            state_estimate = new Matrix(inital_estimate_arr);
            error_estimate = new Matrix(error_estimate_arr);
            innovation = new Matrix(6, 1);
            innovation_covar = new Matrix(6, 6);
            kalman_gain = new Matrix(6, 6);
        }

        public Triplet<Double, Double, Double> update(
                double x, double y, double z, long interval
        ) {
            final double dt = (double) interval / 1000;

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
            double measurement_error_arr[][] = {
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1},
                    { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1}
            };

            Matrix state_transition = new Matrix(state_transition_arr);
            //    Matrix observation = new Matrix(observation_arr);
            Matrix measurement_error = new Matrix(measurement_error_arr);
            Matrix process_error = new Matrix(process_error_arr);
            Matrix new_measurement = new Matrix(6,1);
            Matrix identity = Matrix.identity(6, 6);
            //Actual Algorithm

            //State Prediction

            double[][] xyz = {
                    {state_estimate.get(0, 0)},
                    {state_estimate.get(0, 1)},
                    {state_estimate.get(0, 2)},
                    {x},
                    {y},
                    {z}
            };
            new_measurement = new Matrix(xyz);

            state_estimate = state_transition.times(state_estimate);
            error_estimate = state_transition.times(state_estimate.times(state_transition.transpose())).plus(process_error);
            innovation = new_measurement.minus(state_estimate);
            innovation_covar = error_estimate.plus(measurement_error);
            kalman_gain = error_estimate.times(innovation_covar.inverse());
            state_estimate = state_estimate.plus(kalman_gain.times(innovation));
            error_estimate = identity.minus(kalman_gain).times(error_estimate);

            return new Triplet<Double, Double, Double>(
                    state_estimate.get(0, 0),
                    state_estimate.get(0, 1),
                    state_estimate.get(0, 2)
            );
        }
    }
}
