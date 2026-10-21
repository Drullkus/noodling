package us.drullk.noodling.noodler;

import org.jspecify.annotations.Nullable;
import tamaized.beanification.Component;
import tamaized.beanification.Directory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PastaShapeLookup implements Iterable<PastaShape> {

    private final Map<String, PastaShape> all;
    private final List<PastaShape> pastaShapeList;

    public PastaShapeLookup(@Directory(PastaShape.class) List<PastaShape> pastaShapeList) {
        this.pastaShapeList = pastaShapeList;
        this.all = pastaShapeList.stream().collect(Collectors.toUnmodifiableMap(PastaShape::id, Function.identity()));
    }

    public @Nullable PastaShape byId(String id) {
        return this.all.get(id);
    }

    public Map<String, PastaShape> getAll() {
        return this.all;
    }

    @Override
    public Iterator<PastaShape> iterator() {
        return this.pastaShapeList.iterator();
    }

}
