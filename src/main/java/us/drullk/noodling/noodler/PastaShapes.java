package us.drullk.noodling.noodler;

import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import tamaized.beanification.Component;
import tamaized.beanification.Directory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PastaShapes implements Iterable<PastaShape> {

    private final Map<String, PastaShape> all;
    private final List<PastaShape> pastaShapeList;
    private final int size;

    public PastaShapes(@Directory(PastaShape.class) List<PastaShape> pastaShapeList) {
        this.pastaShapeList = pastaShapeList.stream().sorted().toList();
        this.size = this.pastaShapeList.size();
        this.all = this.pastaShapeList.stream().collect(Collectors.toUnmodifiableMap(PastaShape::id, Function.identity()));
    }

    public @Nullable PastaShape byId(String id) {
        return this.all.get(id);
    }

    public List<PastaShape> getList() {
        return this.pastaShapeList;
    }

    public int size() {
        return this.size;
    }

    public PastaShape getRandom(RandomSource random) {
        return Util.getRandom(this.pastaShapeList, random);
    }

    @Override
    public Iterator<PastaShape> iterator() {
        return this.pastaShapeList.iterator();
    }

}
