INSERT INTO bet (
    bet_id,
    user_id,
    event_id,
    event_market_id,
    event_winner_id,
    bet_amount,
    created_at
)
VALUES
    (
        'BET-1001',
        'USER-1',
        'EVT-1001',
        'MARKET-1',
        'TEAM-A',
        25.00,
        TIMESTAMP WITH TIME ZONE '2026-04-22 10:15:30+00:00'
    ),
    (
        'BET-1002',
        'USER-2',
        'EVT-1001',
        'MARKET-1',
        'TEAM-B',
        10.00,
        TIMESTAMP WITH TIME ZONE '2026-04-22 10:20:30+00:00'
    ),
    (
        'BET-2001',
        'USER-3',
        'EVT-2001',
        'MARKET-2',
        'TEAM-C',
        5.50,
        TIMESTAMP WITH TIME ZONE '2026-04-22 10:25:30+00:00'
    ),
    (
        'BET-3001',
        'USER-4',
        'EVT-3001',
        'MARKET-3',
        'TEAM-D',
        18.75,
        TIMESTAMP WITH TIME ZONE '2026-04-22 10:30:30+00:00'
    );

