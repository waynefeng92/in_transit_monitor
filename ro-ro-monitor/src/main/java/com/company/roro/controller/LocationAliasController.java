package com.company.roro.controller;

import com.company.roro.dto.Result;
import com.company.roro.entity.LocationAlias;
import com.company.roro.service.LocationAliasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location-alias")
public class LocationAliasController {

    @Autowired
    private LocationAliasService locationAliasService;

    @GetMapping
    public Result<List<LocationAlias>> list() {
        return Result.success(locationAliasService.list());
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody LocationAlias alias) {
        return Result.success(locationAliasService.save(alias));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody LocationAlias alias) {
        return Result.success(locationAliasService.updateById(alias));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Integer id) {
        return Result.success(locationAliasService.removeById(id));
    }
}
