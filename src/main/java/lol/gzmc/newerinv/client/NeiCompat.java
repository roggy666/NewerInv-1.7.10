package lol.gzmc.newerinv.client;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Optional NotEnoughItems integration. While the recipe book is open on one of our screens, ask NEI
 * to hide its item panel / bookmark panel / subset bar / search box so they do not overlap the book
 * or eat the horizontal space the combined window needs.
 *
 * <p>Done purely through reflection and a dynamic proxy, so there is no compile-time or hard runtime
 * dependency on NEI: if NEI is absent, or its API has shifted, {@link #tryRegister()} is a silent
 * no-op. This does not stop NEI's per-frame guiLeft re-centering (a separate code path); the panel
 * layout already copes with that.
 */
public final class NeiCompat {

    private NeiCompat() {}

    private static boolean registered = false;
    private static boolean drawHandlerRegistered = false;

    /**
     * Lazily add an {@code IContainerDrawHandler} that re-applies our desired guiLeft in
     * {@code onPreDraw} -- right after NEI's LayoutManager has centered it and before vanilla
     * {@code GuiContainer.drawScreen} snapshots guiLeft into a local. Call this from a book GUI's
     * initGui so NEI is fully loaded and its LayoutManager is already ahead of us in the handler
     * list (handlers run in registration order). No-op without NEI.
     */
    public static void registerDrawHandler() {
        if (drawHandlerRegistered) {
            return;
        }
        try {
            Class<?> mgrCls = Class.forName("codechicken.nei.guihook.GuiContainerManager");
            Class<?> drawItf = Class.forName("codechicken.nei.guihook.IContainerDrawHandler");

            Object handler = Proxy.newProxyInstance(
                    NeiCompat.class.getClassLoader(),
                    new Class<?>[] { drawItf },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("onPreDraw".equals(name)) {
                                Object gui = args[0];
                                if (gui instanceof GuiInventoryBook && BookPanel.isOpen()) {
                                    ((GuiInventoryBook) gui).applyDesiredPosition();
                                }
                                return null;
                            }
                            if ("equals".equals(name)) {
                                return proxy == args[0];
                            }
                            if ("hashCode".equals(name)) {
                                return System.identityHashCode(proxy);
                            }
                            if ("toString".equals(name)) {
                                return "NewerInv$NeiDrawHandler";
                            }
                            // renderObjects / postRenderObjects / renderSlotUnderlay / renderSlotOverlay
                            return null;
                        }
                    });

            mgrCls.getMethod("addDrawHandler", drawItf).invoke(null, handler);
            drawHandlerRegistered = true;
        } catch (Throwable ignored) {
            // NEI absent or incompatible - the inventory just will not slide aside under NEI.
        }
    }

    public static void tryRegister() {
        if (registered) {
            return;
        }
        try {
            Class<?> apiCls = Class.forName("codechicken.nei.api.API");
            Class<?> handlerItf = Class.forName("codechicken.nei.api.INEIGuiHandler");
            final Class<?> visCls = Class.forName("codechicken.nei.VisiblityData");

            Object handler = Proxy.newProxyInstance(
                    NeiCompat.class.getClassLoader(),
                    new Class<?>[] { handlerItf },
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("modifyVisiblity".equals(name)) {
                                Object gui = args[0];
                                Object vis = args[1];
                                if (vis != null && BookPanel.isOpen()
                                        && (gui instanceof GuiInventoryBook || gui instanceof GuiCraftingBook)) {
                                    set(visCls, vis, "showItemPanel", false);
                                    set(visCls, vis, "showBookmarkPanel", false);
                                    set(visCls, vis, "showSubsetDropdown", false);
                                    set(visCls, vis, "showSearchSection", false);
                                }
                                return vis;
                            }
                            if ("handleDragNDrop".equals(name) || "hideItemPanelSlot".equals(name)) {
                                return Boolean.FALSE;
                            }
                            if ("equals".equals(name)) {
                                return proxy == args[0];
                            }
                            if ("hashCode".equals(name)) {
                                return System.identityHashCode(proxy);
                            }
                            if ("toString".equals(name)) {
                                return "NewerInv$NeiGuiHandler";
                            }
                            // getItemSpawnSlots / getInventoryAreas -> null, as INEIGuiAdapter does.
                            return null;
                        }
                    });

            apiCls.getMethod("registerNEIGuiHandler", handlerItf).invoke(null, handler);
            registered = true;
        } catch (Throwable ignored) {
            // NEI not installed or incompatible - nothing to integrate with.
        }
    }

    private static void set(Class<?> visCls, Object vis, String field, boolean value) {
        try {
            Field f = visCls.getField(field);
            f.setBoolean(vis, value);
        } catch (Throwable ignored) {
        }
    }
}
