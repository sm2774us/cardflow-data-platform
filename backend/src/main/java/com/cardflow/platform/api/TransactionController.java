package com.cardflow.platform.api;

import com.cardflow.platform.domain.Records.PageResult;
import com.cardflow.platform.domain.TransactionView;
import com.cardflow.platform.repo.TransactionRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionRepository repo;

    public TransactionController(TransactionRepository repo) { this.repo = repo; }

    @GetMapping
    public PageResult<TransactionView> list(@RequestParam(required = false) String decision,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "25") int size) {
        return repo.search(decision, q, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }
}
