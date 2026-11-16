package ch.realcinema.registry;

import ch.realcinema.RealCinema;
import ch.realcinema.menu.ControlPanelMenu;
import ch.realcinema.menu.FilmBurnerMenu;
import ch.realcinema.menu.FilmCutterMenu;
import ch.realcinema.menu.ProjectorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RealCinema.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<FilmBurnerMenu>> FILM_BURNER =
            MENUS.register("film_burner", () -> IMenuTypeExtension.create(FilmBurnerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ProjectorMenu>> PROJECTOR =
            MENUS.register("projector", () -> IMenuTypeExtension.create(ProjectorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ControlPanelMenu>> CONTROL_PANEL =
            MENUS.register("control_panel", () -> IMenuTypeExtension.create(ControlPanelMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FilmCutterMenu>> FILM_CUTTER =
            MENUS.register("film_cutter", () -> IMenuTypeExtension.create(FilmCutterMenu::new));

    private ModMenus() {}
}
