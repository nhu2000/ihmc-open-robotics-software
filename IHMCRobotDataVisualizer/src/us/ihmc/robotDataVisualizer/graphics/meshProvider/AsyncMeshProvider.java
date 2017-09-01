package us.ihmc.robotDataVisualizer.graphics.meshProvider;

import javafx.scene.shape.MeshView;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AsyncMeshProvider extends MeshProvider
{
   private AtomicReference<List<MeshView>> asyncMeshes = new AtomicReference<>();

   public void provideLater(List<MeshView> meshes) {
      this.asyncMeshes.set(meshes);
   }

   @Override protected List<MeshView> provideMeshes()
   {
      return this.asyncMeshes.getAndSet(null);
   }
}