package net.sharewire.mapsclustering.google;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.sharewire.mapsclustering.Cluster;
import net.sharewire.mapsclustering.ClusterItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sharewire.mapsclustering.Preconditions.checkNotNull;

class GoogleClusterRenderer<T extends ClusterItem> implements GoogleMap.OnMarkerClickListener {

    private static final int BACKGROUND_MARKER_Z_INDEX = 0;

    private static final int FOREGROUND_MARKER_Z_INDEX = 1;

    private final GoogleMap mGoogleMap;

    private final List<Cluster<T>> mClusters = new ArrayList<>();

    private final Map<Cluster<T>, Marker> mMarkers = new HashMap<>();

    private GoogleIconGenerator<T> mIconGenerator;

    private GoogleClusterManager.Callbacks<T> mCallbacks;

    GoogleClusterRenderer(@NonNull Context context, @NonNull GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMarkerClickListener(this);
        mIconGenerator = new GoogleDefaultIconGenerator<>(context);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object markerTag = marker.getTag();
        if (markerTag instanceof Cluster) {
            //noinspection unchecked
            Cluster<T> cluster = (Cluster<T>) marker.getTag();
            //noinspection ConstantConditions
            List<T> clusterItems = cluster.getItems();

            if (mCallbacks != null) {
                if (clusterItems.size() > 1) {
                    return mCallbacks.onClusterClick(cluster);
                } else {
                    return mCallbacks.onClusterItemClick(clusterItems.get(0));
                }
            }
        }

        return false;
    }

    void setCallbacks(@Nullable GoogleClusterManager.Callbacks<T> listener) {
        mCallbacks = listener;
    }

    void setIconGenerator(@NonNull GoogleIconGenerator<T> iconGenerator) {
        mIconGenerator = iconGenerator;
    }

    void render(@NonNull List<Cluster<T>> clusters) {
        List<Cluster<T>> clustersToAdd = new ArrayList<>();
        List<Cluster<T>> clustersToRemove = new ArrayList<>();

        for (Cluster<T> cluster : clusters) {
            if (!mMarkers.containsKey(cluster)) {
                clustersToAdd.add(cluster);
            }
        }

        for (Cluster<T> existingCluster : mMarkers.keySet()) {
            int indexOfExistingCluster = clusters.indexOf(existingCluster);
            boolean newClustersContainsExistingCluster = indexOfExistingCluster >= 0;

            if (!newClustersContainsExistingCluster) {
                clustersToRemove.add(existingCluster);
            } else {
                Cluster<T> clusterWithNewData = clusters.get(indexOfExistingCluster);
                boolean itemsAreEqual = existingCluster.getItems().containsAll(clusterWithNewData.getItems());
                if(!itemsAreEqual) {
                    clustersToRemove.add(existingCluster);
                    clustersToAdd.add(clusterWithNewData);
                }
            }
        }

        mClusters.addAll(clustersToAdd);
        mClusters.removeAll(clustersToRemove);

        // Remove the old clusters.
        for (Cluster<T> clusterToRemove : clustersToRemove) {
            Marker markerToRemove = mMarkers.get(clusterToRemove);
            markerToRemove.setZIndex(BACKGROUND_MARKER_Z_INDEX);

            Cluster<T> parentCluster = findParentCluster(mClusters, clusterToRemove.getLatitude(),
                    clusterToRemove.getLongitude());
            if (parentCluster != null) {
                animateMarkerToLocation(markerToRemove, new LatLng(parentCluster.getLatitude(),
                        parentCluster.getLongitude()), true);
            } else {
                markerToRemove.remove();
            }

            mMarkers.remove(clusterToRemove);
        }

        // Add the new clusters.
        for (Cluster<T> clusterToAdd : clustersToAdd) {
            Marker markerToAdd;

            BitmapDescriptor markerIcon = getMarkerIcon(clusterToAdd);
            String markerTitle = getMarkerTitle(clusterToAdd);
            String markerSnippet = getMarkerSnippet(clusterToAdd);

            Cluster parentCluster = findParentCluster(clustersToRemove, clusterToAdd.getLatitude(),
                    clusterToAdd.getLongitude());
            if (parentCluster != null) {
                markerToAdd = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(parentCluster.getLatitude(), parentCluster.getLongitude()))
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX));
                animateMarkerToLocation(markerToAdd,
                        new LatLng(clusterToAdd.getLatitude(), clusterToAdd.getLongitude()), false);
            } else {
                markerToAdd = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(clusterToAdd.getLatitude(), clusterToAdd.getLongitude()))
                        .icon(markerIcon)
                        .title(markerTitle)
                        .snippet(markerSnippet)
                        .alpha(0.0F)
                        .zIndex(FOREGROUND_MARKER_Z_INDEX));
                animateMarkerAppearance(markerToAdd);
            }
            markerToAdd.setTag(clusterToAdd);

            mMarkers.put(clusterToAdd, markerToAdd);
        }
    }

    @NonNull
    private BitmapDescriptor getMarkerIcon(@NonNull Cluster<T> cluster) {
        BitmapDescriptor clusterIcon;

        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            clusterIcon = mIconGenerator.getClusterIcon(cluster);
        } else {
            clusterIcon = mIconGenerator.getClusterItemIcon(clusterItems.get(0));
        }

        return checkNotNull(clusterIcon);
    }

    @Nullable
    private String getMarkerTitle(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getTitle();
        }
    }

    @Nullable
    private String getMarkerSnippet(@NonNull Cluster<T> cluster) {
        List<T> clusterItems = cluster.getItems();
        if (clusterItems.size() > 1) {
            return null;
        } else {
            return clusterItems.get(0).getSnippet();
        }
    }

    @Nullable
    private Cluster<T> findParentCluster(@NonNull List<Cluster<T>> clusters,
                                         double latitude, double longitude) {
        for (Cluster<T> cluster : clusters) {
            if (cluster.contains(latitude, longitude)) {
                return cluster;
            }
        }

        return null;
    }

    private void animateMarkerToLocation(@NonNull final Marker marker, @NonNull LatLng targetLocation,
                                         final boolean removeAfter) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofObject(marker, "position",
                new LatLngTypeEvaluator(), targetLocation);
        objectAnimator.setInterpolator(new FastOutSlowInInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (removeAfter) {
                    marker.remove();
                }
            }
        });
        objectAnimator.start();
    }

    private void animateMarkerAppearance(@NonNull Marker marker) {
        ObjectAnimator.ofFloat(marker, "alpha", 1.0F).start();
    }

    private static class LatLngTypeEvaluator implements TypeEvaluator<LatLng> {

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            double latitude = (endValue.latitude - startValue.latitude) * fraction + startValue.latitude;
            double longitude = (endValue.longitude - startValue.longitude) * fraction + startValue.longitude;
            return new LatLng(latitude, longitude);
        }
    }
}
