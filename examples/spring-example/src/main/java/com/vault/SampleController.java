package com.vault;

import com.vault.domain.Sample;
import com.vault.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {


    @Autowired
    private SampleRepository sampleRepository;

    @RequestMapping("/samples")
    public Iterable<Sample> samples() {
        final Iterable<Sample> all = sampleRepository.findAll();
        return all;
    }


}