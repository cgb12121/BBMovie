import React, { useState } from 'react';
import { AutoComplete, Input, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import debounce from 'lodash/debounce';
import { apiCall } from '../services/apiWrapper';

const SearchWrapper = styled.div`
    width: 100%;
    max-width: 600px;
`;

interface Movie {
    id: string;
    title: string;
    description: string;
    posterUrl: string;
    rating: number;
}

interface SearchBarProps {
    onSelect?: (movie: Movie) => void;
    onSearch: (value: string) => void;
    placeholder?: string;
    loading?: boolean;
}

interface AutoCompleteOption {
    value: string;
    label: React.ReactNode;
    movie: Movie;
}

const SearchBar: React.FC<SearchBarProps> = ({ onSelect, onSearch, placeholder, loading = false }) => {
    const [options, setOptions] = useState<AutoCompleteOption[]>([]);
    const [isLoading, setIsLoading] = useState(loading);

    const searchMovies = async (query: string, limit?: number) => {
        if (!query) {
            setOptions([]);
            return;
        }

        setIsLoading(true);
        try {
            const response = await apiCall.searchMovies(query, limit);
            if (response.success) {
                const movies = response.data;

                setOptions(movies.map(movie => ({
                    value: String(movie.id),
                    label: (
                        <div key={movie.id} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                            <img
                                src={movie.posterUrl}
                                alt={movie.title}
                                style={{ width: 40, height: 60, objectFit: 'cover' }}
                            />
                            <div>
                                <div>{movie.title}</div>
                                <div style={{ fontSize: '12px', color: '#666' }}>
                                    Rating: {movie.rating}/10
                                </div>
                            </div>
                        </div>
                    ),
                    movie: {
                        id: String(movie.id),
                        title: movie.title,
                        description: movie.description || '',
                        posterUrl: movie.posterUrl,
                        rating: movie.rating
                    }
                })));
            }
        } catch (error) {
            console.error('Search failed:', error);
        } finally {
            setIsLoading(false);
        }
    };


    const debouncedSearch = debounce(searchMovies, 300);

    return (
        <SearchWrapper>
            <AutoComplete
                options={options}
                onSearch={(value) => {
                    debouncedSearch(value);
                    onSearch(value);
                }}
                onSelect={(value, option) => onSelect?.(option.movie)}
                style={{ width: '100%' }}
            >
                <Input
                    size="large"
                    placeholder={placeholder ?? "Search movies..."}
                    prefix={isLoading ? <Spin size="small" /> : <SearchOutlined />}
                />
            </AutoComplete>
        </SearchWrapper>
    );
};

export default SearchBar;
