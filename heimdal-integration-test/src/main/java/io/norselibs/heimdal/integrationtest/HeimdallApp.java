package io.norselibs.heimdal.integrationtest;

import io.odinjector.OdinJector;
import io.varhttp.Standalone;
import io.varhttp.VarConfig;

public class HeimdallApp {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        OdinJector odinJector = OdinJector.create()
                .addContext(new HeimdallContext(new VarConfig().setPort(port)));

        Standalone standalone = odinJector.getInstance(Standalone.class);
        standalone.configure(config -> {
            config.setObjectFactory(odinJector::getInstance);
            config.addParameterHandler(VarHeimdalParameterHandler.class);
            config.addController(BikeFormController.class);
            config.addController(StaticController.class);
        });

        System.out.println("Heimdal integration test at http://localhost:" + port + "/bikes/new");
        standalone.run();
    }
}
