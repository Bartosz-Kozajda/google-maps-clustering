package net.sharewire.mapsclustering.huawei;

import androidx.annotation.NonNull;


import com.huawei.hms.maps.model.BitmapDescriptor;

import net.sharewire.mapsclustering.Cluster;
import net.sharewire.mapsclustering.ClusterItem;

/**
 * Generates icons for clusters and cluster items. Note that its implementations
 * should cache generated icons for subsequent use. For the example implementation see
 * {@link DefaultIconGenerator}.
 */
public interface HuaweiIconGenerator<T extends ClusterItem> {
    /**
     * Returns an icon for the given cluster.
     *
     * @param cluster the cluster to return an icon for
     * @return the icon for the given cluster
     */
    @NonNull
    BitmapDescriptor getClusterIcon(@NonNull Cluster<T> cluster);

    /**
     * Returns an icon for the given cluster item.
     *
     * @param clusterItem the cluster item to return an icon for
     * @return the icon for the given cluster item
     */
    @NonNull
    BitmapDescriptor getClusterItemIcon(@NonNull T clusterItem);
}
