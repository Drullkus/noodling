package us.drullk.noodling.noodler;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.Component;
import tamaized.beanification.Directory;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PastaShapeLookup {

    private final Map<String, PastaShape> all;

    public PastaShapeLookup(@Directory(PastaShape.class) List<PastaShape> pastaShapeList) {
        this.all = pastaShapeList.stream().collect(Collectors.toUnmodifiableMap(PastaShape::id, Function.identity()));
    }

    public @Nullable PastaShape byId(String id) {
        return this.all.get(id);
    }

    public void forEach(BiConsumer<? super String, ? super PastaShape> action) {
        this.all.forEach(action);
    }

    public Map<String, PastaShape> getAll() {
        return this.all;
    }

}
