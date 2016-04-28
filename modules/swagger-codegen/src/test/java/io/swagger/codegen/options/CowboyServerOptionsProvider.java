package io.swagger.codegen.options;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class CowboyServerOptionsProvider implements OptionsProvider {
    @Override
    public String getLanguage() {
        return "cowboy";
    }

    @Override
    public Map<String, String> createOptions() {
        //SinatraServerCodegen doesn't have its own options and base options are cleared
        return ImmutableMap.of();
    }

    @Override
    public boolean isServer() {
        return true;
    }
}
