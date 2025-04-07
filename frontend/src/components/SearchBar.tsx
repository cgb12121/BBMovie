import React, { useState, useEffect, useCallback } from 'react';
import { Input, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import debounce from 'lodash/debounce';

const { Search } = Input;

interface SearchBarProps {
    placeholder?: string;
    onSearch: (value: string) => void;
    loading?: boolean;
    debounceTime?: number;
    className?: string;
}

const SearchBarContainer = styled.div`
    position: relative;
    max-width: 500px;
    margin: 0 auto;
`;

const LoadingIndicator = styled.div`
    position: absolute;
    right: 40px;
    top: 50%;
    transform: translateY(-50%);
`;

const SearchBar: React.FC<SearchBarProps> = ({
    placeholder = 'Search...',
    onSearch,
    loading = false,
    debounceTime = 300,
    className
}) => {
    const [searchValue, setSearchValue] = useState('');

    const debouncedSearch = useCallback(
        debounce((value: string) => {
            onSearch(value);
        }, debounceTime),
        [onSearch, debounceTime]
    );

    useEffect(() => {
        return () => {
            debouncedSearch.cancel();
        };
    }, [debouncedSearch]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = e.target.value;
        setSearchValue(value);
        debouncedSearch(value);
    };

    const handleSearch = (value: string) => {
        setSearchValue(value);
        onSearch(value);
    };

    return (
        <SearchBarContainer className={className}>
            <Search
                placeholder={placeholder}
                allowClear
                enterButton={<SearchOutlined />}
                size="large"
                value={searchValue}
                onChange={handleChange}
                onSearch={handleSearch}
            />
            {loading && (
                <LoadingIndicator>
                    <Spin size="small" />
                </LoadingIndicator>
            )}
        </SearchBarContainer>
    );
};

export default SearchBar; 